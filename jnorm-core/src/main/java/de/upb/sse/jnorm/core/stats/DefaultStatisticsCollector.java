package de.upb.sse.jnorm.core.stats;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of StatisticsCollector. This implementation is thread-safe and uses
 * ConcurrentHashMap for storing statistics.
 */
@Slf4j
public class DefaultStatisticsCollector implements StatisticsCollector {
  private final ProcessingStats stats = new ProcessingStats();
  private final Map<String, NormalizerStats> normalizerStats = new HashMap<>();

  public DefaultStatisticsCollector() {}

  @Override
  public void recordNormalization(
      String className, String type, boolean success, String errorMessage) {
    // Only record method normalizers
    if (type.startsWith("jnorm.core.normalization.methodnormalizers.")) {
      String normalizer = type.substring(type.lastIndexOf(".") + 1);
      normalizerStats.computeIfAbsent(normalizer, k -> new NormalizerStats()).recordNormalization();
      // Record per-class stats
      stats.recordAppliedNormalizer(className, normalizer);
      log.debug("Applied normalizer {} to class {}", normalizer, className);
    }

    // Record success or failure
    if (success) {
      stats.addSuccessfulNormalization(className, type);
    } else {
      stats.addFailedNormalization(className, type, errorMessage);
    }
  }

  @Override
  public void recordProcessing(String className, boolean success, String errorMessage) {
    if (success) {
      stats.recordProcessedClass(className, true);
      log.debug("Successfully processed class: {}", className);
    } else if (errorMessage != null) {
      // First record it as a processed class (not identical)
      stats.recordProcessedClass(className, false);
      // Then record the error
      ProcessingError error =
          new ProcessingError(
              className,
              ProcessingError.ErrorPhase.FRAMEWORK_PROCESSING,
              "PROCESSING_FAILED",
              errorMessage,
              null);
      stats.recordError(className, error);
      log.error("Failed to process class: {}", className);
    } else {
      // This is a difference, not an error
      stats.recordProcessedClass(className, false);
      log.debug("Class {} has differences", className);
      stats
          .getAppliedNormalizers(className)
          .forEach(
              normalizer ->
                  normalizerStats
                      .computeIfAbsent(normalizer, k -> new NormalizerStats())
                      .recordNormalization());
      log.info(
          "Class {} is different with {} normalizers applied",
          className,
          stats.getAppliedNormalizers(className).size());
    }
  }

  @Override
  public ProcessingStats getStatistics() {
    stats.setNormalizerStats(normalizerStats);
    return stats;
  }

  @Override
  public void reset() {
    stats.reset();
    log.info("Statistics collector has been reset");
  }
}
