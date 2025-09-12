package de.upb.sse.jnorm.core.normalization.classnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedClassTracker;
import java.util.HashSet;
import java.util.Set;
import sootup.core.model.MethodModifier;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class EnumValuesModifierNormalizer implements ClassNormalizer {
  private final ModifiedClassTracker tracker = new ModifiedClassTracker();

  public JavaSootClass normalize(JavaSootClass clazz) {
    if (!clazz.isEnum()) return clazz;

    Set<JavaSootMethod> methods = clazz.getMethods();
    Set<JavaSootMethod> normalizedMethods = new HashSet<>();
    boolean modified = false;

    for (JavaSootMethod method : methods) {
      if (!method.getName().equals("values")) {
        normalizedMethods.add(method);
        continue;
      }

      Set<MethodModifier> methodModifiers = new HashSet<>(method.getModifiers());
      if (methodModifiers.remove(MethodModifier.SYNTHETIC)) {
        modified = true;
      }

      JavaSootMethod normalizedMethod = method.withModifiers(methodModifiers);
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
