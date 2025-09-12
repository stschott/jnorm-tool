package de.upb.sse.jnorm.core.stats;

import lombok.Getter;

/** Statistics for a specific normalizer. */
@Getter
public class NormalizerStats {
  private int count;

  public NormalizerStats() {
    this.count = 0;
  }

  public void recordNormalization() {
    count++;
  }
}
