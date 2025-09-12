package de.upb.sse.jnorm.core.io;

import java.io.IOException;
import sootup.java.core.JavaSootClass;

public interface JimpleWriter {
  void write(JavaSootClass javaClass, String outputPath) throws IOException;
}
