package de.upb.sse.jnorm.core.stats;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Statistics for processing operations. Tracks success/failure and timing information for class
 * processing and normalization.
 */
@Slf4j
@Getter
public class ProcessingStats {

  @Setter private Map<String, NormalizerStats> normalizerStats;
  private final Map<String, ClassDifference> differences;
  private final Map<String, Set<String>> appliedNormalizers;
  private final Set<String> differentClasses;
  private final Set<String> disjunctionClasses;
  private final Map<String, ProcessingError> errors;

  @Setter private int totalProcessedClasses; // Total number of classes processed
  @Setter private int identicalClasses; // Classes that are exactly the same after normalization
  @Setter private int errorClasses; // Classes that failed due to processing errors
  @Setter private double errorPercentage; // Percentage of classes that failed processing
  @Setter private double similarityPercentage; // Percentage of classes that are identical

  public ProcessingStats() {
    this.normalizerStats = new HashMap<>();
    this.differences = new HashMap<>();
    this.appliedNormalizers = new HashMap<>();
    this.differentClasses = new HashSet<>();
    this.disjunctionClasses = new HashSet<>();
    this.errors = new HashMap<>();

    this.totalProcessedClasses = 0;
    this.identicalClasses = 0;
    this.errorClasses = 0;
    this.errorPercentage = 0.0;
    this.similarityPercentage = 0.0;
  }

  /**
   * Record a processed class and update statistics.
   *
   * @param className Name of the class
   * @param identical Whether the class is identical to its reference
   */
  public void recordProcessedClass(String className, boolean identical) {
    totalProcessedClasses++;
    if (identical) {
      identicalClasses++;
      differentClasses.remove(className);
      differences.remove(className);
    } else {
      differentClasses.add(className);
      differences.put(className, new ClassDifference(className, 0.0));
    }
    updatePercentages();
  }

  /**
   * Record that a normalizer was applied to a class
   *
   * @param className The name of the class
   * @param normalizerName The name of the normalizer
   */
  public void recordAppliedNormalizer(String className, String normalizerName) {
    Set<String> normalizers = appliedNormalizers.computeIfAbsent(className, k -> new HashSet<>());
    normalizers.add(normalizerName);
    log.debug(
        "Added normalizer {} to class {}. Total normalizers for class: {}",
        normalizerName,
        className,
        normalizers.size());
  }

  /**
   * Get the set of normalizers that were applied to a class
   *
   * @param className The name of the class
   * @return Set of normalizer names, empty if none were applied
   */
  public Set<String> getAppliedNormalizers(String className) {
    return appliedNormalizers.getOrDefault(className, Collections.emptySet());
  }

  /**
   * Record a class that exists in only one version.
   *
   * @param className Name of the class
   */
  public void recordDisjunctionClass(String className) {
    disjunctionClasses.add(className);
  }

  /**
   * Record a processing error for a class.
   *
   * @param className Name of the class
   * @param error Error details
   */
  public void recordError(String className, ProcessingError error) {
    // Don't increment totalProcessedClasses here since it's already incremented in
    // recordProcessedClass
    errorClasses++;
    errors.put(className, error);
    updatePercentages();
  }

  public void addSuccessfulNormalization(String className, String type) {
    // Don't record as a processed class here, that's done in recordProcessing
  }

  public void addFailedNormalization(String className, String type, String errorMessage) {
    // Don't record as a processed class here, that's done in recordProcessing
    recordError(className, new ProcessingError(errorMessage));
  }

  private void updatePercentages() {
    if (totalProcessedClasses > 0) {
      errorPercentage =
          Math.round(((double) errorClasses / totalProcessedClasses) * 10000.0) / 100.0;
      similarityPercentage =
          Math.round(((double) identicalClasses / totalProcessedClasses) * 10000.0) / 100.0;
    }
  }

  /** Reset all statistics to their initial values. */
  public void reset() {
    totalProcessedClasses = 0;
    identicalClasses = 0;
    errorClasses = 0;
    errorPercentage = 0.0;
    similarityPercentage = 0.0;
    differentClasses.clear();
    disjunctionClasses.clear();
    errors.clear();
    normalizerStats.clear();
  }
}
