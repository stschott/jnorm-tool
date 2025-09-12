package de.upb.sse.jnorm.core.benchmark;

import de.upb.sse.jnorm.core.config.BenchmarkConfig;
import de.upb.sse.jnorm.core.config.CoreConfig;
import de.upb.sse.jnorm.core.processing.SourceProcessor;
import de.upb.sse.jnorm.core.processing.SourceProcessorFactory;
import de.upb.sse.jnorm.core.stats.AggregatedStatsManager;
import de.upb.sse.jnorm.core.stats.DefaultStatisticsCollector;
import de.upb.sse.jnorm.core.stats.ProcessingStats;
import de.upb.sse.jnorm.core.stats.StatisticsReporter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.model.SootClass;
import sootup.core.util.printer.JimplePrinter;
import sootup.java.core.JavaSootClass;

/** Main runner class for executing benchmarks. */
@Slf4j
public class BenchmarkRunner {
  private static final Logger logger = LoggerFactory.getLogger(BenchmarkRunner.class);

  private final BenchmarkConfig config;
  @Getter private final SourceProcessor plainProcessor;
  @Getter private final SourceProcessor normalizingProcessor;
  private final Set<String> excludedClasses;
  private final AggregatedStatsManager aggregatedStats;

  public BenchmarkRunner(BenchmarkConfig config, Set<String> excludedClasses) {
    this.config = config;
    this.excludedClasses = excludedClasses;
    this.aggregatedStats = new AggregatedStatsManager();

    // Create processors
    DefaultStatisticsCollector plainCollector = new DefaultStatisticsCollector();
    DefaultStatisticsCollector normalizingCollector = new DefaultStatisticsCollector();
    this.plainProcessor = createPlainProcessor(plainCollector);
    this.normalizingProcessor = createNormalizingProcessor(normalizingCollector);
  }

  /**
   * Compare two versions of a project.
   *
   * @param projectName The name of the project
   * @param v1 Version 1 identifier
   * @param v2 Version 2 identifier
   * @param v1Path Path to version 1
   * @param v2Path Path to version 2
   * @throws IOException if there is an error writing statistics
   */
  public void compareVersions(String projectName, String v1, String v2, Path v1Path, Path v2Path)
      throws IOException {
    logger.info("Comparing {} vs {} in {}", v1, v2, projectName);

    // Update config with version info
    BenchmarkConfig versionConfig =
        new BenchmarkConfig.Builder()
            .setOutputDirectory(config.getOutputDirectory())
            .setStatsDirectory(config.getStatsDirectory())
            .setGenerateDetailedStats(config.isGenerateDetailedStats())
            .setCompareNormalizedOnly(config.isCompareNormalizedOnly())
            .setSourceVersion(v1)
            .setTargetVersion(v2)
            .build();

    // Create collectors for this project
    DefaultStatisticsCollector plainCollector = new DefaultStatisticsCollector();
    DefaultStatisticsCollector normalizedCollector = new DefaultStatisticsCollector();

    // Create processors for this project
    SourceProcessor projectPlainProcessor = createPlainProcessor(plainCollector);
    SourceProcessor projectNormalizedProcessor = createNormalizingProcessor(normalizedCollector);

    // Process classes from both versions
    Stream<JavaSootClass> classes1 = projectPlainProcessor.process(v1Path.toString());
    Stream<JavaSootClass> classes2 = projectPlainProcessor.process(v2Path.toString());
    Stream<JavaSootClass> normalizedClasses1 =
        projectNormalizedProcessor.process(v1Path.toString());
    Stream<JavaSootClass> normalizedClasses2 =
        projectNormalizedProcessor.process(v2Path.toString());

    // Compare plain and normalized versions
    if (!versionConfig.isCompareNormalizedOnly()) {
      compareAndReport("plain", projectName, versionConfig, classes1, classes2, plainCollector);
    }
    compareAndReport(
        "normalized",
        projectName,
        versionConfig,
        normalizedClasses1,
        normalizedClasses2,
        normalizedCollector);
  }

  private String classToString(JavaSootClass clazz) {
    JimplePrinter printer = new JimplePrinter(JimplePrinter.Option.Deterministic);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    printer.printTo(clazz, printWriter);
    printWriter.close();
    return stringWriter.toString();
  }

  private void compareAndReport(
      String type,
      String projectName,
      BenchmarkConfig versionConfig,
      Stream<JavaSootClass> classes1,
      Stream<JavaSootClass> classes2,
      DefaultStatisticsCollector collector)
      throws IOException {
    // Collect classes into sets for comparison
    Set<JavaSootClass> classes1Set = classes1.collect(Collectors.toSet());
    Set<JavaSootClass> classes2Set = classes2.collect(Collectors.toSet());

    // Compare class contents and collect differences
    logger.info("----- Diffs -----");

    // Reset collector for this comparison
    collector.reset();

    // Find disjunction (classes present in only one version)
    Set<String> disjunctionClasses = getDisjunction(classes1Set, classes2Set);

    logger.info("----- Disjunction -----");
    disjunctionClasses.forEach(
        className -> {
          logger.info(className);
        });

    // Compare each class from version 1 that exists in version 2
    for (JavaSootClass clazz1 : classes1Set) {
      String clazz1Name = clazz1.getName();

      // Skip excluded classes
      if (excludedClasses.contains(clazz1Name)) {
        continue;
      }

      try {
        // Find matching class in version 2
        Optional<JavaSootClass> clazz2 =
            classes2Set.stream().filter(c -> c.getName().equals(clazz1Name)).findFirst();

        if (clazz2.isPresent()) {
          String clazz1String = classToString(clazz1);
          String clazz2String = classToString(clazz2.get());

          if (!clazz1String.equals(clazz2String)) {
            logger.info("Class {} differs", clazz1Name);
            collector.recordProcessing(clazz1Name, false, null);
          } else {
            logger.debug("Class {} matches", clazz1Name);
            collector.recordProcessing(clazz1Name, true, null);
          }
        } else {
          // Class exists in version 1 but not in version 2
          logger.info("Class {} not found in version 2", clazz1Name);
          disjunctionClasses.add(clazz1Name);
          collector.recordProcessing(clazz1Name, false, null);
        }
      } catch (IllegalArgumentException e) {
        logger.error("Error comparing class {}: {}", clazz1Name, e.getMessage());
        collector.recordProcessing(clazz1Name, false, e.getMessage());
      }
    }

    // Get current stats for reporting
    ProcessingStats currentStats = collector.getStatistics();

    // Add disjunction classes to stats
    for (String className : disjunctionClasses) {
      currentStats.recordDisjunctionClass(className);
    }

    // Create stats directory if it doesn't exist
    Path statsDir = config.getStatsDirectory();
    if (!statsDir.toFile().exists()) {
      statsDir.toFile().mkdirs();
    }

    // Write detailed statistics to JSON, including normalizer stats
    String statsFileName =
        String.format(
            "%s_%s_%s_vs_%s.json",
            projectName, type, versionConfig.getSourceVersion(), versionConfig.getTargetVersion());
    StatisticsReporter reporter = new StatisticsReporter(statsDir);
    reporter.writeToJson(currentStats, statsDir.resolve(statsFileName));

    // Add to aggregated stats if this is a normalized comparison
    if (type.equals("normalized")) {
      aggregatedStats.addProjectStats(
          projectName,
          versionConfig.getSourceVersion(),
          versionConfig.getTargetVersion(),
          currentStats);
    }

    // Get final stats for reporting
    ProcessingStats finalStats = collector.getStatistics();

    // Log summary
    logger.info("Comparison Summary for {} view:", type);
    logger.info("Total classes compared: {}", finalStats.getTotalProcessedClasses());
    logger.info(
        "Matching classes: {} ({}%)",
        finalStats.getIdenticalClasses(),
        String.format(
            "%.2f",
            finalStats.getTotalProcessedClasses() > 0
                ? (finalStats.getIdenticalClasses() * 100.0 / finalStats.getTotalProcessedClasses())
                : 0));
    logger.info("Differing classes: {}", finalStats.getDifferentClasses().size());
    logger.info("Classes in disjunction: {}", disjunctionClasses.size());
  }

  private Set<String> getDisjunction(Set<JavaSootClass> classes1, Set<JavaSootClass> classes2) {
    Set<String> disjunction = new HashSet<>();
    Set<String> names1 = classes1.stream().map(SootClass::getName).collect(Collectors.toSet());
    Set<String> names2 = classes2.stream().map(SootClass::getName).collect(Collectors.toSet());

    // Add classes unique to version 1
    names1.stream().filter(name -> !names2.contains(name)).forEach(disjunction::add);

    // Add classes unique to version 2
    names2.stream().filter(name -> !names1.contains(name)).forEach(disjunction::add);

    return disjunction;
  }

  private SourceProcessor createPlainProcessor(DefaultStatisticsCollector collector) {
    CoreConfig coreConfig =
        new CoreConfig.Builder()
            .setOptimizationEnabled(false)
            .setNormalizationEnabled(false)
            .setAggressiveNormalizationEnabled(false)
            .build();
    return SourceProcessorFactory.createSourceProcessor(coreConfig, collector);
  }

  private SourceProcessor createNormalizingProcessor(DefaultStatisticsCollector collector) {
    CoreConfig coreConfig =
        new CoreConfig.Builder()
            .setOptimizationEnabled(true)
            .setNormalizationEnabled(true)
            .setAggressiveNormalizationEnabled(true)
            .build();
    return SourceProcessorFactory.createSourceProcessor(coreConfig, collector);
  }

  /**
   * Write aggregated statistics for all version comparisons.
   *
   * @param statsDir Directory to write stats to
   * @throws IOException if writing fails
   */
  public void writeAggregatedStats(Path statsDir) throws IOException {
    // Write project aggregated stats
    String aggregatedFileName = "aggregated_stats.json";
    aggregatedStats.writeToJson(statsDir.resolve(aggregatedFileName));
    logger.info("Written aggregated stats to {}", aggregatedFileName);
  }
}
