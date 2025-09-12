package de.upb.sse.jnorm.core.normalization.classnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedClassTracker;
import java.util.*;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class AnonymousConstructorExceptionRemover implements ClassNormalizer {
  private final ModifiedClassTracker tracker = new ModifiedClassTracker();

  @Override
  public JavaSootClass normalize(JavaSootClass clazz) {
    final String ANONYMOUS_CLASS_REGEX = ".*\\$\\d+$";
    final String INIT = "<init>";

    String className = clazz.getName();
    if (!className.matches(ANONYMOUS_CLASS_REGEX)) return clazz;

    Set<JavaSootMethod> methods = clazz.getMethods();
    Set<JavaSootMethod> normalizedMethods = new HashSet<>();
    boolean modified = false;

    for (JavaSootMethod method : methods) {
      if (!method.getName().equals(INIT)) {
        normalizedMethods.add(method);
        continue;
      }

      // Now we have a constructor
      JavaSootMethod normalizedMethod = method.withThrownExceptions(Collections.emptyList());
      normalizedMethods.add(normalizedMethod);
      modified = true;
    }

    if (modified) {
      tracker.recordModification(clazz.getName());
    }

    return clazz.withMethods(normalizedMethods);
  }

  public Set<String> getModifiedClasses() {
    return tracker.getModifiedClasses();
  }
}
