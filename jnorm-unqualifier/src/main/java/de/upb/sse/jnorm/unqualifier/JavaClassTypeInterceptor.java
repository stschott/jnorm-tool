package de.upb.sse.jnorm.unqualifier;

import java.util.concurrent.atomic.AtomicBoolean;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import sootup.java.core.types.JavaClassType;

/**
 * Intercepts and temporarily overrides the behavior of the JavaClassType class to modify its
 * behavior dynamically during runtime.
 */
public class JavaClassTypeInterceptor {

  private final ClassReloadingStrategy strategy;
  private final AtomicBoolean isModified = new AtomicBoolean(false);

  public JavaClassTypeInterceptor() {
    ByteBuddyAgent.install();
    this.strategy = ClassReloadingStrategy.fromInstalledAgent();
  }

  private void enableReplacement() {
    if (!isModified.compareAndSet(false, true)) return;

    try (var unloaded =
        new ByteBuddy()
            .redefine(JavaClassType.class)
            .method(
                ElementMatchers.named("getFullyQualifiedName")
                    .or(ElementMatchers.named("toString"))
                    .and(ElementMatchers.isPublic()))
            .intercept(MethodDelegation.to(JavaClassTypeMethodHandler.class))
            .make()) {
      unloaded.load(JavaClassType.class.getClassLoader(), strategy);
    } catch (Exception e) {
      throw new RuntimeException("Failed to enable method interception for JavaClassType", e);
    }
  }

  private void disableReplacement() {
    if (!isModified.compareAndSet(true, false)) return;

    try (var unloaded =
        new ByteBuddy()
            .redefine(
                JavaClassType.class,
                ClassFileLocator.ForClassLoader.of(JavaClassType.class.getClassLoader()))
            .make()) {
      unloaded.load(JavaClassType.class.getClassLoader(), strategy);
    } catch (Exception e) {
      throw new RuntimeException("Failed to disable replacement", e);
    }
  }

  /**
   * Executes a given task while temporarily overriding the behavior of the JavaClassType class.
   *
   * @param task The task to execute while the JavaClassType behavior is modified.
   */
  public void intercept(Runnable task) {
    enableReplacement();
    try {
      task.run();
    } finally {
      disableReplacement();
    }
  }

  /** Defines the custom behavior for intercepted methods in the JavaClassType class. */
  public static class JavaClassTypeMethodHandler {
    @SuppressWarnings("unused")
    @RuntimeType
    public static String intercept(@This JavaClassType instance) {
      return instance.getClassName();
    }
  }
}
