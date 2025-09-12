package de.upb.sse.jnorm.core.stats;

/**
 * Interface for collecting statistics about normalization and processing operations.
 * Implementations should be thread-safe as they may be accessed from multiple threads.
 */
public interface StatisticsCollector {
  /**
   * Record a normalization attempt.
   *
   * @param className The name of the class being normalized
   * @param type The type of normalization (e.g., "TrapNormalizer", "ControlFlowNormalizer")
   * @param success Whether the normalization was successful
   * @param errorMessage Error message if the normalization failed
   */
  void recordNormalization(String className, String type, boolean success, String errorMessage);

  /**
   * Record the processing of a class.
   *
   * @param className The name of the class being processed
   * @param success Whether the processing was successful
   * @param errorMessage Error message if the processing failed
   */
  void recordProcessing(String className, boolean success, String errorMessage);

  /**
   * Get the current statistics.
   *
   * @return The current statistics snapshot
   */
  ProcessingStats getStatistics();

  /** Reset all statistics to their initial state. */
  void reset();
}
