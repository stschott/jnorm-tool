package de.upb.sse.jnorm.core.stats;

import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;

/**
 * Statistics for a specific type of normalization. Tracks success/failure counts and timing
 * information.
 */
public class NormalizationStats {
  @Getter private final String type;
  private final AtomicLong successCount;
  private final AtomicLong failureCount;
  private final AtomicLong totalDurationMs;
  private final AtomicLong minDurationMs;
  private final AtomicLong maxDurationMs;

  public NormalizationStats(String type) {
    this.type = type;
    this.successCount = new AtomicLong(0);
    this.failureCount = new AtomicLong(0);
    this.totalDurationMs = new AtomicLong(0);
    this.minDurationMs = new AtomicLong(Long.MAX_VALUE);
    this.maxDurationMs = new AtomicLong(0);
  }

  public void recordResult(boolean success, long durationMs) {
    if (success) {
      successCount.incrementAndGet();
    } else {
      failureCount.incrementAndGet();
    }
    totalDurationMs.addAndGet(durationMs);

    // Update min duration using CAS to ensure thread safety
    long currentMin;
    do {
      currentMin = minDurationMs.get();
      if (durationMs >= currentMin) break;
    } while (!minDurationMs.compareAndSet(currentMin, durationMs));

    // Update max duration using CAS to ensure thread safety
    long currentMax;
    do {
      currentMax = maxDurationMs.get();
      if (durationMs <= currentMax) break;
    } while (!maxDurationMs.compareAndSet(currentMax, durationMs));
  }

  public long getTotalCount() {
    return successCount.get() + failureCount.get();
  }

  public double getAverageTime() {
    long total = getTotalCount();
    return total == 0 ? 0.0 : (double) totalDurationMs.get() / total;
  }
}
