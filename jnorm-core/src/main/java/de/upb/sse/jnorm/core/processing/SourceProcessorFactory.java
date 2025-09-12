package de.upb.sse.jnorm.core.processing;

import de.upb.sse.jnorm.core.config.CoreConfig;
import de.upb.sse.jnorm.core.config.ProcessingConfig;
import de.upb.sse.jnorm.core.exceptions.ConfigurationException;
import de.upb.sse.jnorm.core.normalization.AggressiveMethodNormalizer;
import de.upb.sse.jnorm.core.normalization.MethodNormalizerWrapper;
import de.upb.sse.jnorm.core.normalization.classnormalizers.AnonymousConstructorExceptionRemover;
import de.upb.sse.jnorm.core.normalization.classnormalizers.ClassModifierNormalizer;
import de.upb.sse.jnorm.core.normalization.classnormalizers.ClassNormalizer;
import de.upb.sse.jnorm.core.normalization.classnormalizers.ConstructorModifierNormalizer;
import de.upb.sse.jnorm.core.normalization.classnormalizers.EnumValuesMethodRemover;
import de.upb.sse.jnorm.core.normalization.classnormalizers.EnumValuesModifierNormalizer;
import de.upb.sse.jnorm.core.normalization.classnormalizers.SyntheticMethodRemover;
import de.upb.sse.jnorm.core.normalization.methodnormalizers.*;
import de.upb.sse.jnorm.core.sootup.CustomTypeAssigner;
import de.upb.sse.jnorm.core.sootup.SootUpWrapper;
import de.upb.sse.jnorm.core.stats.DefaultStatisticsCollector;
import de.upb.sse.jnorm.core.stats.StatisticsCollector;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import sootup.core.transform.BodyInterceptor;
import sootup.interceptors.*;

/** Factory class for creating SourceProcessor instances based on configuration. */
@Slf4j
public class SourceProcessorFactory {

  /**
   * Creates a new SourceProcessor with the given configuration.
   *
   * @param config The configuration to use for creating the processor
   * @return A new SourceProcessor instance
   * @throws ConfigurationException if the configuration is invalid
   */
  public static SourceProcessor createSourceProcessor(ProcessingConfig config) {
    return createSourceProcessor(config, new DefaultStatisticsCollector());
  }

  /**
   * Creates a new SourceProcessor with the given configuration and statistics collector.
   *
   * @param config The configuration to use for creating the processor
   * @param statisticsCollector The statistics collector to use
   * @return A new SourceProcessor instance
   * @throws ConfigurationException if the configuration is invalid
   */
  public static SourceProcessor createSourceProcessor(
      ProcessingConfig config, StatisticsCollector statisticsCollector) {
    Objects.requireNonNull(config, "ProcessingConfig cannot be null");
    Objects.requireNonNull(statisticsCollector, "StatisticsCollector cannot be null");

    log.debug(
        "Creating SourceProcessor with config: optimization={}, normalization={}, aggressiveNormalization={}",
        config.isOptimizationEnabled(),
        config.isNormalizationEnabled(),
        config.isAggressiveNormalizationEnabled());

    try {
      SootUpWrapper sootUpWrapper = new SootUpWrapper();
      List<BodyInterceptor> enabledInterceptors = new ArrayList<>();
      List<BodyInterceptor> methodInterceptors = createMethodInterceptors();
      List<BodyInterceptor> methodNormalizers = createMethodNormalizers();

      if (!config.isOptimizationEnabled()) {
        enabledInterceptors.add(new LocalSplitter());
        enabledInterceptors.add(new Aggregator());
        enabledInterceptors.add(new CustomTypeAssigner());
      }

      if (config.isOptimizationEnabled()) {
        log.debug("Adding method interceptors for optimization");
        enabledInterceptors.addAll(methodInterceptors);
      }

      // Modify the methodNormalizers list if aggressive normalization is disabled
      if (!config.isAggressiveNormalizationEnabled()) {
        log.debug("Removing aggressive normalizers");
        methodNormalizers.removeIf(
            interceptor -> {
              boolean isAggressive = interceptor instanceof AggressiveMethodNormalizer;
              if (isAggressive) {
                log.debug(
                    "Removing aggressive normalizer: {}", interceptor.getClass().getSimpleName());
              }
              return isAggressive;
            });
      }

      // Add methodNormalizers to methodInterceptors if normalization is enabled
      if (config.isNormalizationEnabled() || config.isAggressiveNormalizationEnabled()) {
        log.debug("Adding method normalizers");
        enabledInterceptors.addAll(methodNormalizers);
      }

      List<ClassNormalizer> classNormalizers = createClassNormalizers(config);
      log.debug(
          "Created SourceProcessor with {} interceptors and {} normalizers",
          enabledInterceptors.size(),
          classNormalizers.size());

      enabledInterceptors =
          enabledInterceptors.stream()
              .map(
                  interceptor ->
                      (BodyInterceptor)
                          new MethodNormalizerWrapper(interceptor, statisticsCollector))
              .toList();

      return new SourceProcessor(
          config, statisticsCollector, sootUpWrapper, enabledInterceptors, classNormalizers);
    } catch (Exception e) {
      String msg = "Failed to create SourceProcessor: " + e.getMessage();
      log.error(msg, e);
      throw new ConfigurationException(msg, e);
    }
  }

  /**
   * Creates a new SourceProcessor with the given core configuration and statistics collector. This
   * is a convenience method that creates a ProcessingConfig from the CoreConfig.
   *
   * @param config The core configuration to use for creating the processor
   * @param statisticsCollector The statistics collector to use
   * @return A new SourceProcessor instance
   * @throws ConfigurationException if the configuration is invalid
   */
  public static SourceProcessor createSourceProcessor(
      CoreConfig config, StatisticsCollector statisticsCollector) {
    ProcessingConfig processingConfig =
        new ProcessingConfig.Builder()
            .applyOptimizations(config.isOptimizationEnabled())
            .applyNormalization(config.isNormalizationEnabled())
            .applyAggressiveNormalization(config.isAggressiveNormalizationEnabled())
            .build();
    return createSourceProcessor(processingConfig, statisticsCollector);
  }

  private static List<BodyInterceptor> createMethodInterceptors() {
    return new ArrayList<>(
        Arrays.asList(
            new NopEliminator(),
            new EmptySwitchEliminator(),
            new CastAndReturnInliner(),
            new LocalSplitter(),
            new Aggregator(),
            new ConstantPropagatorAndFolder(),
            new CustomTypeAssigner(),
            new DeadAssignmentEliminator(),
            new UnreachableCodeEliminator()));
  }

  private static List<BodyInterceptor> createMethodNormalizers() {
    return new ArrayList<>(
        Arrays.asList(
            new ArithmeticNormalizer(),
            new BufferMethodCallNormalizer(),
            new CharSequenceToStringNormalizer(),
            new DuplicateTypeCastEliminator(),
            new EmptyAnonymousClassEliminator(),
            new EnumNormalizer(),
            new NestBasedAccessNormalizer(),
            new NullCheckNormalizer(),
            new PrivateInvokeNormalizer(),
            new RedundantTrapEliminator(),
            new TrapNormalizer(),
            new DynamicInvokeNormalizer(),
            new StringConcatNormalizer(),
            new ConcurrentHashMapNormalizer(),
            new TypeCastEliminator(),
            new DeadAssignmentEliminator(),
            new UnreachableCodeEliminator(),
            new CustomUnusedLocalNormalizer(),
            new CustomLocalNameStandardizer()));
  }

  private static List<ClassNormalizer> createClassNormalizers(ProcessingConfig config) {
    List<ClassNormalizer> classNormalizers = new ArrayList<>();
    if (config.isNormalizationEnabled() || config.isAggressiveNormalizationEnabled()) {
      log.debug("Adding class normalizers");
      classNormalizers.addAll(
          Arrays.asList(
              new SyntheticMethodRemover(),
              new EnumValuesMethodRemover(),
              new ClassModifierNormalizer(),
              new ConstructorModifierNormalizer(),
              new EnumValuesModifierNormalizer(),
              new AnonymousConstructorExceptionRemover()));
    }
    return classNormalizers;
  }
}
