package de.upb.sse.jnorm.core.normalization.classnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedClassTracker;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import sootup.core.model.MethodModifier;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class ConstructorModifierNormalizer implements ClassNormalizer {
  private final ModifiedClassTracker tracker = new ModifiedClassTracker();

  public JavaSootClass normalize(JavaSootClass clazz) {
    if (clazz.isAnnotation()) return clazz;
    final String INIT = "<init>";

    Set<JavaSootMethod> methods = clazz.getMethods();
    Set<JavaSootMethod> normalizedMethods = new HashSet<>();
    boolean modified = false;

    for (JavaSootMethod method : methods) {
      if (!method.getName().equals(INIT)) {
        normalizedMethods.add(method);
        continue;
      }

      // Now we have a constructor
      Set<MethodModifier> methodModifiers = new HashSet<>(method.getModifiers());
      if (methodModifiers.remove(MethodModifier.PRIVATE)) {
        modified = true;
      }

      JavaSootMethod normalizedMethod =
          method.withAnnotations(Collections.emptyList()).withModifiers(methodModifiers);
      normalizedMethods.add(normalizedMethod);
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
