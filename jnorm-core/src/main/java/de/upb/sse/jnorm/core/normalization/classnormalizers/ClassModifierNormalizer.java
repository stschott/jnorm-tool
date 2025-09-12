package de.upb.sse.jnorm.core.normalization.classnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedClassTracker;
import java.util.Set;
import sootup.core.model.ClassModifier;
import sootup.java.core.JavaSootClass;

public class ClassModifierNormalizer implements ClassNormalizer {
  private final ModifiedClassTracker tracker = new ModifiedClassTracker();

  public JavaSootClass normalize(JavaSootClass sc) {
    Set<ClassModifier> newModifiers = sc.getModifiers();
    boolean modified = false;

    if (newModifiers.remove(ClassModifier.FINAL)) {
      modified = true;
    }
    if (newModifiers.remove(ClassModifier.SYNTHETIC)) {
      modified = true;
    }

    if (modified) {
      tracker.recordModification(sc.getName());
    }

    return sc.withModifiers(newModifiers);
  }

  public Set<String> getModifiedClasses() {
    return tracker.getModifiedClasses();
  }
}
