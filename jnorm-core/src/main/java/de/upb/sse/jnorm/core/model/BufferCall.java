package de.upb.sse.jnorm.core.model;

import sootup.core.jimple.common.Local;
import sootup.core.types.ClassType;

public class BufferCall {
  Local local;
  ClassType buffer;

  public BufferCall(Local local, ClassType buffer) {
    this.local = local;
    this.buffer = buffer;
  }

  public Local getLocal() {
    return local;
  }

  public ClassType getBuffer() {
    return buffer;
  }

  @Override
  public String toString() {
    return local + ": " + buffer.getFullyQualifiedName();
  }
}
