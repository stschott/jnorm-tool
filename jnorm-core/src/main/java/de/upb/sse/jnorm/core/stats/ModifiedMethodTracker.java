package de.upb.sse.jnorm.core.stats;

import java.util.HashSet;
import java.util.Set;

/** Simple tracker for methods modified by normalizers. */
public class ModifiedMethodTracker {
  private final Set<String> modifiedMethods = new HashSet<>();

  public void recordModification(String methodSignature) {
    modifiedMethods.add(methodSignature);
  }

  public Set<String> getModifiedMethods() {
    return new HashSet<>(modifiedMethods);
  }
}
