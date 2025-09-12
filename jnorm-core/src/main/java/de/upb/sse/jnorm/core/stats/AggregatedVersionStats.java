package de.upb.sse.jnorm.core.stats;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;

/** Aggregated statistics for comparing specific Java versions across multiple projects. */
@Getter
public class AggregatedVersionStats {
  private final String sourceVersion;
  private final String targetVersion;
  private final Set<String> projects;
  private final AtomicInteger totalClasses;
  private final AtomicInteger identicalClasses;
  private final AtomicInteger differentClasses;
  private final AtomicInteger disjunctionClasses;
  private final AtomicInteger errorClasses;
  private double errorPercentage;
  private double overallSimilarityPercentage;

  public AggregatedVersionStats(String sourceVersion, String targetVersion) {
    this.sourceVersion = sourceVersion;
    this.targetVersion = targetVersion;
    this.projects = new HashSet<>();
    this.totalClasses = new AtomicInteger(0);
    this.identicalClasses = new AtomicInteger(0);
    this.differentClasses = new AtomicInteger(0);
    this.disjunctionClasses = new AtomicInteger(0);
    this.errorClasses = new AtomicInteger(0);
    this.errorPercentage = 0.0;
    this.overallSimilarityPercentage = 0.0;
  }

  /**
   * Merge another project's stats into this aggregated stats object.
   *
   * @param projectName Name of the project
   * @param stats Stats to merge
   */
  public void mergeStats(String projectName, ProcessingStats stats) {
    projects.add(projectName);
    totalClasses.addAndGet(stats.getTotalProcessedClasses());
    identicalClasses.addAndGet(stats.getIdenticalClasses());
    differentClasses.addAndGet(stats.getDifferentClasses().size());
    disjunctionClasses.addAndGet(stats.getDisjunctionClasses().size());
    errorClasses.addAndGet(stats.getErrorClasses());

    updatePercentages();
  }

  private void updatePercentages() {
    int totalProcessedClasses = totalClasses.get();
    if (totalProcessedClasses > 0) {
      errorPercentage =
          Math.round(((double) errorClasses.get() / totalProcessedClasses) * 10000.0) / 100.0;
      overallSimilarityPercentage =
          Math.round(((double) identicalClasses.get() / totalProcessedClasses) * 10000.0) / 100.0;
    }
  }
}
