package de.upb.sse.jnorm.core.stats;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/** Manages aggregated statistics across multiple projects and version comparisons. */
@Slf4j
public class AggregatedStatsManager {
  private final Map<String, AggregatedVersionStats> versionStats;

  public AggregatedStatsManager() {
    this.versionStats = new ConcurrentHashMap<>();
  }

  /**
   * Add stats from a project comparison to the aggregated stats.
   *
   * @param projectName Name of the project
   * @param sourceVersion Source Java version
   * @param targetVersion Target Java version
   * @param stats Stats to add
   */
  public void addProjectStats(
      String projectName, String sourceVersion, String targetVersion, ProcessingStats stats) {
    String key = getVersionKey(sourceVersion, targetVersion);
    AggregatedVersionStats aggregated =
        versionStats.computeIfAbsent(
            key, k -> new AggregatedVersionStats(sourceVersion, targetVersion));
    aggregated.mergeStats(projectName, stats);
    log.info(
        "Added stats from project {} comparing {} to {}",
        projectName,
        sourceVersion,
        targetVersion);
  }

  /**
   * Write aggregated stats to a JSON file.
   *
   * @param outputPath Path to write the JSON file
   * @throws IOException if writing fails
   */
  public JsonObject toJson() {
    JsonObject root = new JsonObject();
    JsonArray comparisons = new JsonArray();

    // Calculate overall totals
    int totalClasses = 0;
    int totalIdenticalClasses = 0;
    int totalDifferentClasses = 0;
    int totalDisjunctionClasses = 0;
    int totalErrorClasses = 0;

    for (AggregatedVersionStats stats : versionStats.values()) {
      JsonObject comparison = new JsonObject();
      comparison.addProperty("sourceVersion", stats.getSourceVersion());
      comparison.addProperty("targetVersion", stats.getTargetVersion());

      // Add project list
      JsonArray projects = new JsonArray();
      stats.getProjects().forEach(projects::add);
      comparison.add("projects", projects);

      int statsTotal = stats.getTotalClasses().get();
      int statsIdentical = stats.getIdenticalClasses().get();
      int statsDifferent = stats.getDifferentClasses().get();
      int statsDisjunction = stats.getDisjunctionClasses().get();
      int statsErrors = stats.getErrorClasses().get();

      comparison.addProperty("totalClasses", statsTotal);
      comparison.addProperty("identicalClasses", statsIdentical);
      comparison.addProperty("differentClasses", statsDifferent);
      comparison.addProperty("disjunctionClasses", statsDisjunction);
      comparison.addProperty("errorClasses", statsErrors);
      comparison.addProperty("errorPercentage", stats.getErrorPercentage());
      comparison.addProperty("overallSimilarityPercentage", stats.getOverallSimilarityPercentage());

      // Add to overall totals
      totalClasses += statsTotal;
      totalIdenticalClasses += statsIdentical;
      totalDifferentClasses += statsDifferent;
      totalDisjunctionClasses += statsDisjunction;
      totalErrorClasses += statsErrors;

      comparisons.add(comparison);
    }

    root.add("versionComparisons", comparisons);

    // Add overall totals section
    JsonObject overall = new JsonObject();
    overall.addProperty("totalClasses", totalClasses);
    overall.addProperty("identicalClasses", totalIdenticalClasses);
    overall.addProperty("differentClasses", totalDifferentClasses);
    overall.addProperty("disjunctionClasses", totalDisjunctionClasses);
    overall.addProperty("errorClasses", totalErrorClasses);

    // Calculate overall percentages
    double overallErrorPercentage =
        totalClasses > 0
            ? Math.round(((double) totalErrorClasses / totalClasses) * 10000.0) / 100.0
            : 0.0;
    double overallSimilarityPercentage =
        totalClasses > 0
            ? Math.round(((double) totalIdenticalClasses / totalClasses) * 10000.0) / 100.0
            : 0.0;

    overall.addProperty("errorPercentage", overallErrorPercentage);
    overall.addProperty("overallSimilarityPercentage", overallSimilarityPercentage);

    root.add("overall", overall);
    return root;
  }

  /**
   * Write aggregated stats to a JSON file.
   *
   * @param outputPath Path to write the JSON file
   * @throws IOException if writing fails
   */
  public void writeToJson(Path outputPath) throws IOException {
    try (FileWriter writer = new FileWriter(outputPath.toFile())) {
      new GsonBuilder().setPrettyPrinting().create().toJson(toJson(), writer);
    }
  }

  private String getVersionKey(String sourceVersion, String targetVersion) {
    return sourceVersion + "_to_" + targetVersion;
  }
}
