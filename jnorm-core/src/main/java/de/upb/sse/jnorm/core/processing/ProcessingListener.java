package de.upb.sse.jnorm.core.processing;

import sootup.java.core.JavaSootClass;

/** Listener interface for processing pipeline events. */
public interface ProcessingListener {
  /**
   * Called when a class has been processed.
   *
   * @param before The class before processing
   * @param after The class after processing
   */
  void onClassProcessed(JavaSootClass before, JavaSootClass after);

  /**
   * Called when a normalizer has been applied to a class.
   *
   * @param normalizer The name of the normalizer
   * @param target The target class
   */
  void onNormalizerApplied(String normalizer, JavaSootClass target);
}
