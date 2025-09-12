package de.upb.sse.jnorm.core.stats;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

/**
 * Represents differences between two versions of a class. Tracks both similarity percentage and
 * which normalizers were applied.
 */
@Getter
public class ClassDifference {
  private final String className;
  private final double similarityPercentage;
  private final Set<String> appliedMethodNormalizers;
  private final Set<String> appliedClassNormalizers;

  public ClassDifference(String className, double similarityPercentage) {
    this(className, similarityPercentage, new HashSet<>(), new HashSet<>());
  }

  public ClassDifference(
      String className,
      double similarityPercentage,
      Set<String> methodNormalizers,
      Set<String> classNormalizers) {
    this.className = className;
    this.similarityPercentage = similarityPercentage;
    this.appliedMethodNormalizers = Collections.unmodifiableSet(new HashSet<>(methodNormalizers));
    this.appliedClassNormalizers = Collections.unmodifiableSet(new HashSet<>(classNormalizers));
  }
}
