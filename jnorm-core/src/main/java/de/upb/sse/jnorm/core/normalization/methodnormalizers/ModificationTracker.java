package de.upb.sse.jnorm.core.normalization.methodnormalizers;

import java.util.Set;

/** Interface for normalizers that track which methods they modify. */
public interface ModificationTracker {
  /** Returns the set of method signatures that were modified by this normalizer. */
  Set<String> getModifiedMethods();
}
