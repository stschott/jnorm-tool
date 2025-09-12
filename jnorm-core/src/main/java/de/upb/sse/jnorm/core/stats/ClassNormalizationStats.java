package de.upb.sse.jnorm.core.stats;

import lombok.Getter;

/** Statistics for normalization operations on a specific class. */
@Getter
public class ClassNormalizationStats {
  private int successCount;
  private int failureCount;
  private String lastErrorMessage;

  public ClassNormalizationStats() {
    this.successCount = 0;
    this.failureCount = 0;
    this.lastErrorMessage = null;
  }

  /**
   * Record a normalization operation.
   *
   * @param success Whether the normalization was successful
   * @param errorMessage Error message if the normalization failed
   */
  public void recordNormalization(boolean success, String errorMessage) {
    if (success) {
      successCount++;
    } else {
      failureCount++;
      lastErrorMessage = errorMessage;
    }
  }
}
