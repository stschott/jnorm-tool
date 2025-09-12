package de.upb.sse.jnorm.core.stats;

/** Interface for components that can track their own statistics. */
public interface Trackable {
  /**
   * Get the name of this trackable component.
   *
   * @return The name used for statistics tracking
   */
  String getName();

  /**
   * Get the statistics for this component.
   *
   * @return The statistics object
   */
  NormalizationStats getStats();
}
