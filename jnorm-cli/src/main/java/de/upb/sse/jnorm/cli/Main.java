package de.upb.sse.jnorm.cli;

import de.upb.sse.jnorm.core.config.ProcessingConfig;
import de.upb.sse.jnorm.core.io.JimpleFileWriter;
import de.upb.sse.jnorm.core.processing.JNormRunner;
import de.upb.sse.jnorm.core.stats.DefaultStatisticsCollector;
import de.upb.sse.jnorm.core.stats.StatisticsReporter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.java.core.JavaSootClass;

/** Main entry point for the jNorm CLI application. */
public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    CliHandler cli = new CliHandler();
    Options options = cli.getOptions();

    // Parse command line arguments
    CommandLine cmdLine = parseCommandLine(cli, options, args);
    if (cmdLine == null) {
      return;
    }

    log.info("Running jNorm with configuration: {}", String.join(" ", args));

    try {
      // Set up paths and configuration
      Path inputDir = Paths.get(cmdLine.getOptionValue(CliHandler.inputDirOpt));
      Path outputDir = Paths.get(cmdLine.getOptionValue(CliHandler.outputDirOpt, "output"));
      ProcessingConfig config = createConfig(cmdLine);

      // Create components
      DefaultStatisticsCollector stats = new DefaultStatisticsCollector();
      JNormRunner runner = new JNormRunner(config, stats);
      StatisticsReporter reporter = new StatisticsReporter(outputDir);
      JimpleFileWriter writer = new JimpleFileWriter(outputDir);

      // Process classes
      Stream<JavaSootClass> transformedClasses = runner.process(inputDir);
      writeOutput(transformedClasses, writer);

      // Write statistics
      reporter.writeNormalizerStats(stats.getStatistics());
      log.info("Processing complete. Output written to: {}", outputDir);

    } catch (Exception e) {
      log.error("Processing failed: {}", e.getMessage(), e);
      System.exit(1);
    }
  }

  private static CommandLine parseCommandLine(CliHandler cli, Options options, String[] args) {
    if (args.length < 1
        || Arrays.asList(args).contains("-h")
        || Arrays.asList(args).contains("--help")) {
      cli.showHelpMessage(options);
      return null;
    }

    try {
      return new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      log.error("Failed to parse command line arguments: {}", e.getMessage());
      return null;
    }
  }

  private static ProcessingConfig createConfig(CommandLine cmdLine) {
    return new ProcessingConfig.Builder()
        .applyNormalization(cmdLine.hasOption(CliHandler.normalizationOpt))
        .applyAggressiveNormalization(cmdLine.hasOption(CliHandler.aggressiveOpt))
        .build();
  }

  private static void writeOutput(Stream<JavaSootClass> classes, JimpleFileWriter writer) {
    classes.forEach(
        clazz -> {
          try {
            String filename = String.format("%s.jimple", clazz.getName());
            writer.write(clazz, filename);
          } catch (IOException e) {
            log.error("Failed to write class: {}", clazz.getName(), e);
          }
        });
  }
}
