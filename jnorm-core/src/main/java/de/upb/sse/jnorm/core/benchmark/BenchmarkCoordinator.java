package de.upb.sse.jnorm.core.benchmark;

import de.upb.sse.jnorm.core.config.BenchmarkConfig;
import de.upb.sse.jnorm.core.config.ProcessingConfig;
import de.upb.sse.jnorm.core.processing.DiffComparator;
import de.upb.sse.jnorm.core.processing.DiffComparator.ComparisonResult;
import de.upb.sse.jnorm.core.processing.JNormRunner;
import de.upb.sse.jnorm.core.stats.AggregatedStatsManager;
import de.upb.sse.jnorm.core.stats.DefaultStatisticsCollector;
import de.upb.sse.jnorm.core.stats.ProcessingStats;
import de.upb.sse.jnorm.core.stats.StatisticsReporter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import sootup.java.core.JavaSootClass;

/**
 * Coordinates benchmark operations including processing multiple versions of projects, comparing
 * their outputs, and generating statistics reports.
 */
@Slf4j
public class BenchmarkCoordinator {
  private final BenchmarkConfig config;
  private final Set<String> excludedClasses;
  private final StatisticsReporter statsReporter;
  private final DiffComparator diffComparator;
  private final AggregatedStatsManager aggregatedStats;

  // Runners for different processing modes
  private final ProcessingConfig plainConfig;
  private final ProcessingConfig normalizedConfig;

  @Builder
  public BenchmarkCoordinator(BenchmarkConfig config, Set<String> excludedClasses) {
    this.config = config;
    this.excludedClasses = excludedClasses != null ? excludedClasses : new HashSet<>();
    this.statsReporter = new StatisticsReporter(config.getStatsDirectory());
    this.diffComparator = new DiffComparator(this.excludedClasses);
    this.aggregatedStats = new AggregatedStatsManager();

    // Create configurations
    this.plainConfig = createPlainConfig();
    this.normalizedConfig = createNormalizedConfig();
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
    log.info("Comparing {} vs {} in {}", v1, v2, projectName);

    // Create runners with fresh stats collectors for each version
    JNormRunner plainRunner1 = new JNormRunner(plainConfig, new DefaultStatisticsCollector());
    JNormRunner plainRunner2 = new JNormRunner(plainConfig, new DefaultStatisticsCollector());
    JNormRunner normalizedRunner1 =
        new JNormRunner(normalizedConfig, new DefaultStatisticsCollector());
    JNormRunner normalizedRunner2 =
        new JNormRunner(normalizedConfig, new DefaultStatisticsCollector());

    // Process classes from both versions
    Stream<JavaSootClass> classes1 = plainRunner1.process(v1Path);
    Stream<JavaSootClass> classes2 = plainRunner2.process(v2Path);
    Stream<JavaSootClass> normalizedClasses1 = normalizedRunner1.process(v1Path);
    Stream<JavaSootClass> normalizedClasses2 = normalizedRunner2.process(v2Path);

    // Compare plain versions if not restricted to normalized only
    if (!config.isCompareNormalizedOnly()) {
      compareAndReport(
          "plain",
          projectName,
          v1,
          v2,
          classes1,
          classes2,
          plainRunner1.getStats(),
          plainRunner2.getStats());
    }

    // Compare normalized versions
    compareAndReport(
        "normalized",
        projectName,
        v1,
        v2,
        normalizedClasses1,
        normalizedClasses2,
        normalizedRunner1.getStats(),
        normalizedRunner2.getStats());
  }

  /**
   * Write final statistics after all comparisons are complete.
   *
   * @throws IOException if there is an error writing statistics
   */
  public void writeFinalStats() throws IOException {
    // Write aggregated statistics
    statsReporter.writeAggregatedStats(aggregatedStats);
  }

  private void compareAndReport(
      String type,
      String projectName,
      String v1,
      String v2,
      Stream<JavaSootClass> classes1,
      Stream<JavaSootClass> classes2,
      ProcessingStats stats1,
      ProcessingStats stats2)
      throws IOException {

    // Create a combined stats object for the comparison
    ProcessingStats combinedStats = new ProcessingStats();
    stats1
        .getNormalizerStats()
        .forEach((key, value) -> combinedStats.getNormalizerStats().put(key, value));
    stats2
        .getNormalizerStats()
        .forEach((key, value) -> combinedStats.getNormalizerStats().put(key, value));

    // Compare the classes
    ComparisonResult result = diffComparator.compare(classes1, classes2, combinedStats);

    // Write comparison statistics
    statsReporter.writeComparisonStats(projectName, type, v1, v2, combinedStats);

    // Add to aggregated stats if this is a normalized comparison
    if (type.equals("normalized")) {
      aggregatedStats.addProjectStats(projectName, v1, v2, combinedStats);
    }

    // Log summary
    logComparisonSummary(type, result);
  }

  private void logComparisonSummary(String type, ComparisonResult result) {
    ProcessingStats stats = result.getStats();
    log.info("Comparison Summary for {} view:", type);
    log.info("Total classes compared: {}", stats.getTotalProcessedClasses());
    log.info(
        "Matching classes: {} ({}%)",
        result.getMatchingClasses().size(),
        String.format(
            "%.2f",
            stats.getTotalProcessedClasses() > 0
                ? (stats.getIdenticalClasses() * 100.0 / stats.getTotalProcessedClasses())
                : 0));
    log.info("Differing classes: {}", result.getDifferingClasses().size());
    log.info("Classes in disjunction: {}", result.getDisjunctionClasses().size());
    log.info("Classes with errors: {}", result.getErrorClasses().size());
  }

  private ProcessingConfig createPlainConfig() {
    return ProcessingConfig.builder()
        .applyOptimizations(false)
        .applyNormalization(false)
        .applyAggressiveNormalization(false)
        .build();
  }

  private ProcessingConfig createNormalizedConfig() {
    return ProcessingConfig.builder()
        .applyOptimizations(true)
        .applyNormalization(true)
        .applyAggressiveNormalization(true)
        .build();
  }
}
