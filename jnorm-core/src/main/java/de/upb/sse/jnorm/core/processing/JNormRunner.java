package de.upb.sse.jnorm.core.processing;

import de.upb.sse.jnorm.core.config.ProcessingConfig;
import de.upb.sse.jnorm.core.stats.ProcessingStats;
import de.upb.sse.jnorm.core.stats.StatisticsCollector;
import java.nio.file.Path;
import java.util.stream.Stream;
import lombok.Getter;
import sootup.java.core.JavaSootClass;

/**
 * Core runner for JNorm processing. Handles the normalization of Java bytecode using a configured
 * processor and collects statistics about the process.
 */
public class JNormRunner {
  /** -- GETTER -- Get the configuration used by this runner. */
  @Getter private final ProcessingConfig config;

  @Getter private final StatisticsCollector stats;
  private final SourceProcessor processor;

  /**
   * Creates a new JNormRunner with the specified configuration and statistics collector.
   *
   * @param config The configuration to use for processing
   * @param stats The statistics collector to use
   */
  public JNormRunner(ProcessingConfig config, StatisticsCollector stats) {
    this.config = config;
    this.stats = stats;
    this.processor = SourceProcessorFactory.createSourceProcessor(config, stats);
  }

  /**
   * Process Java bytecode from the specified input path.
   *
   * @param inputPath Path to the directory containing Java bytecode
   * @return Stream of processed JavaSootClass instances
   */
  public Stream<JavaSootClass> process(Path inputPath) {
    return processor.process(inputPath.toString());
  }

  /**
   * Get the current processing statistics.
   *
   * @return The current processing statistics
   */
  public ProcessingStats getStats() {
    return stats.getStatistics();
  }
}
