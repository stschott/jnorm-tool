package de.upb.sse.jnorm.core.normalization.classnormalizers;

import sootup.java.core.JavaSootClass;

public interface ClassNormalizer {

  JavaSootClass normalize(JavaSootClass clazz);
}
