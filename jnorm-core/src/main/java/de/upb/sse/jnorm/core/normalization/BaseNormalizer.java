package de.upb.sse.jnorm.core.normalization;

import de.upb.sse.jnorm.core.stats.NormalizationStats;
import de.upb.sse.jnorm.core.stats.Trackable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for normalizers that provides statistics tracking. */
public abstract class BaseNormalizer implements Trackable {
  private static final Logger logger = LoggerFactory.getLogger(BaseNormalizer.class);
  private final NormalizationStats stats;
  private final String name;

  protected BaseNormalizer() {
    this.name = getClass().getSimpleName();
    this.stats = new NormalizationStats(name);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public NormalizationStats getStats() {
    return stats;
  }

  /**
   * Record a normalization operation.
   *
   * @param success Whether the normalization was successful
   * @param durationMs The duration of the normalization in milliseconds
   * @param errorMessage Optional error message if the normalization failed
   */
  protected void recordNormalization(boolean success, long durationMs, String errorMessage) {
    stats.recordResult(success, durationMs);
    if (!success) {
      logger.warn("Normalization failed for {}: {}", getName(), errorMessage);
    } else {
      logger.debug("Normalization successful for {} in {}ms", getName(), durationMs);
    }
  }
}
