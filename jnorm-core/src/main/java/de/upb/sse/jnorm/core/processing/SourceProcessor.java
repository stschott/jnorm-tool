package de.upb.sse.jnorm.core.processing;

import de.upb.sse.jnorm.core.config.ProcessingConfig;
import de.upb.sse.jnorm.core.exceptions.ProcessingException;
import de.upb.sse.jnorm.core.normalization.classnormalizers.ClassNormalizer;
import de.upb.sse.jnorm.core.sootup.SootUpWrapper;
import de.upb.sse.jnorm.core.stats.StatisticsCollector;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import sootup.core.model.SootClass;
import sootup.core.transform.BodyInterceptor;
import sootup.core.views.View;
import sootup.java.core.JavaSootClass;

@Slf4j
public class SourceProcessor {

  private final ProcessingConfig config;
  private final StatisticsCollector statsCollector;
  private final List<ProcessingListener> listeners;
  private final SootUpWrapper sootUpWrapper;
  private final List<BodyInterceptor> interceptors;
  private final List<ClassNormalizer> normalizers;

  SourceProcessor(
      ProcessingConfig config,
      StatisticsCollector statsCollector,
      SootUpWrapper sootUpWrapper,
      List<BodyInterceptor> interceptors,
      List<ClassNormalizer> normalizers) {
    this.config = config;
    this.statsCollector = statsCollector;
    this.listeners = new ArrayList<>();
    this.sootUpWrapper = sootUpWrapper;
    this.interceptors = new ArrayList<>(interceptors);
    this.normalizers = new ArrayList<>(normalizers);
  }

  public Stream<JavaSootClass> process(String inputDir) {
    if (inputDir == null || inputDir.trim().isEmpty()) {
      throw new ProcessingException("Input directory cannot be null or empty");
    }

    long startTime = System.currentTimeMillis();
    try {
      Optional<View> viewOpt = sootUpWrapper.createViewFromDirectory(inputDir, interceptors);
      if (viewOpt.isEmpty()) {
        log.error("Failed to create view from directory: {}", inputDir);
        return Stream.empty();
      }

      View view = viewOpt.get();
      Stream<JavaSootClass> classes =
          view.getClasses()
              .filter(SootClass::isApplicationClass)
              .filter(cl -> cl instanceof JavaSootClass)
              .map(cl -> (JavaSootClass) cl);

      if (config.isNormalizationEnabled()) {
        classes = classes.map(this::normalizeClass);
      }

      //            statsCollector.recordProcessing(inputDir, true, null);

      return classes;
    } catch (Exception e) {
      statsCollector.recordProcessing(inputDir, false, e.getMessage());
      log.error("Error processing directory: {}", inputDir, e);
      return Stream.empty();
    }
  }

  private JavaSootClass normalizeClass(JavaSootClass clazz) {
    JavaSootClass original = clazz;
    try {
      if (config.isOptimizationEnabled()) {
        // Apply optimizations
        clazz = applyOptimizations(clazz);
      }

      if (config.isNormalizationEnabled()) {
        // Apply normalizations
        clazz = applyNormalizations(clazz);
      }

      String className = clazz.getName();
      statsCollector.recordNormalization(className, "ProcessingNormalizer", true, null);
      notifyClassProcessed(original, clazz);

      return clazz;
    } catch (Exception e) {
      String className = clazz.getName();
      statsCollector.recordNormalization(className, "ProcessingNormalizer", false, e.getMessage());
      log.error("Error normalizing class: {}", clazz.getName(), e);
      return original;
    }
  }

  private JavaSootClass applyOptimizations(JavaSootClass clazz) {
    // TO DO: implement optimizations
    return clazz;
  }

  private JavaSootClass applyNormalizations(JavaSootClass clazz) {
    String currentClassName = clazz.getName();
    JavaSootClass normalizedClass = clazz;
    for (ClassNormalizer normalizer : normalizers) {
      String normalizerName = normalizer.getClass().getSimpleName();
      try {
        normalizedClass = normalizer.normalize(normalizedClass);
        // We don't record class normalizers in appliedNormalizers anymore
        String className = normalizedClass.getName();
        log.debug("Applied normalizer {} to class {}", normalizerName, className);
        notifyNormalizerApplied(normalizerName, normalizedClass);
      } catch (Exception e) {
        statsCollector.recordNormalization(currentClassName, normalizerName, false, e.getMessage());
        log.error("Error during normalization with {}: {}", normalizerName, e.getMessage());
        throw new ProcessingException(
            "Failed to normalize class " + clazz.getName() + " with " + normalizerName, e);
      }
    }
    return normalizedClass;
  }

  private void notifyClassProcessed(JavaSootClass before, JavaSootClass after) {
    for (ProcessingListener listener : listeners) {
      try {
        listener.onClassProcessed(before, after);
      } catch (Exception e) {
        log.error("Error in processing listener: {}", e.getMessage());
      }
    }
  }

  private void notifyNormalizerApplied(String normalizer, JavaSootClass target) {
    for (ProcessingListener listener : listeners) {
      try {
        listener.onNormalizerApplied(normalizer, target);
      } catch (Exception e) {
        log.error("Error in processing listener: {}", e.getMessage());
      }
    }
  }
}
