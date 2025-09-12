package de.upb.sse.jnorm.core.normalization.classnormalizers;

import de.upb.sse.jnorm.core.stats.ModifiedClassTracker;
import java.util.Set;
import java.util.stream.Collectors;
import sootup.core.model.MethodModifier;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class SyntheticMethodRemover implements ClassNormalizer {
  private final ModifiedClassTracker tracker = new ModifiedClassTracker();

  public JavaSootClass normalize(JavaSootClass sc) {
    Set<JavaSootMethod> methods = sc.getMethods();
    Set<JavaSootMethod> nonSyntheticMethods =
        methods.stream()
            .filter(
                method ->
                    method.getName().equals("$values")
                        || !method.getModifiers().contains(MethodModifier.SYNTHETIC)
                            && !method.getModifiers().contains(MethodModifier.BRIDGE))
            .collect(Collectors.toSet());

    if (nonSyntheticMethods.size() != methods.size()) {
      tracker.recordModification(sc.getName());
    }

    return sc.withMethods(nonSyntheticMethods);
  }

  public Set<String> getModifiedClasses() {
    return tracker.getModifiedClasses();
  }
}
