package de.upb.sse.jnorm.core.normalizers;

import de.upb.sse.jnorm.core.stats.NormalizerStats;

/** Interface for components that can track their own statistics. */
public interface Trackable {
  /**
   * Get the name of this trackable component.
   *
   * @return The component name
   */
  String getName();

  /**
   * Get the statistics for this component.
   *
   * @return The component's statistics
   */
  NormalizerStats getStats();
}
