package de.upb.sse.jnorm.core.normalization.classnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedClassTracker;
import java.util.Set;
import java.util.stream.Collectors;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class EnumValuesMethodRemover implements ClassNormalizer {
  private final ModifiedClassTracker tracker = new ModifiedClassTracker();

  public JavaSootClass normalize(JavaSootClass clazz) {
    if (!clazz.isEnum()) return clazz;

    Set<JavaSootMethod> methods = clazz.getMethods();
    Set<JavaSootMethod> filteredMethods =
        methods.stream()
            .filter(method -> !method.getName().equals("$values"))
            .collect(Collectors.toSet());

    if (filteredMethods.size() != methods.size()) {
      tracker.recordModification(clazz.getName());
    }

    return clazz.withMethods(filteredMethods);
  }

  public Set<String> getModifiedClasses() {
    return tracker.getModifiedClasses();
  }
}
