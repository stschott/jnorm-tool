package de.upb.sse.jnorm.core.stats;

import java.util.HashSet;
import java.util.Set;

/** Simple tracker for classes modified by normalizers. */
public class ModifiedClassTracker {
  private final Set<String> modifiedClasses = new HashSet<>();

  public void recordModification(String className) {
    modifiedClasses.add(className);
  }

  public Set<String> getModifiedClasses() {
    return new HashSet<>(modifiedClasses);
  }
}
