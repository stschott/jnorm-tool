package de.upb.sse.jnorm.core.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** Reports statistics. Handles both project-specific and normalizer statistics. */
@Slf4j
public class StatisticsReporter {
  private final Path outputDirectory;

  private static final Gson staticGson = new GsonBuilder().setPrettyPrinting().create();

  /**
   * Create a new StatisticsReporter that writes to the given directory.
   *
   * @param outputDirectory Directory to write reports to
   */
  public StatisticsReporter(Path outputDirectory) {
    this.outputDirectory = outputDirectory;

    try {
      Files.createDirectories(outputDirectory);
    } catch (IOException e) {
      log.error("Failed to create output directory: {}", outputDirectory, e);
      throw new RuntimeException("Failed to create output directory", e);
    }
  }

  /**
   * Write comparison statistics for a specific project version comparison.
   *
   * @param projectName Name of the project
   * @param type Type of comparison (e.g., "plain", "normalized")
   * @param sourceVersion Source version identifier
   * @param targetVersion Target version identifier
   * @param stats The statistics to write
   * @throws IOException if there is an error writing the file
   */
  public void writeComparisonStats(
      String projectName,
      String type,
      String sourceVersion,
      String targetVersion,
      ProcessingStats stats)
      throws IOException {
    String filename =
        String.format("%s_%s_%s_vs_%s.json", projectName, type, sourceVersion, targetVersion);
    Path outputPath = outputDirectory.resolve(filename);
    writeToJson(stats, outputPath);
    log.info("Written comparison stats to {}", outputPath);
  }

  /**
   * Write JSON to a file.
   *
   * @param json The JSON to write
   * @param outputPath The path to write to
   * @throws IOException if there is an error writing
   */
  private void writeJsonToFile(JsonObject json, Path outputPath) throws IOException {
    try (FileWriter writer = new FileWriter(outputPath.toFile())) {
      staticGson.toJson(json, writer);
    }
  }

  /**
   * Write normalizer statistics to a separate JSON file.
   *
   * @param stats The statistics containing normalizer information
   * @throws IOException if there is an error writing the file
   */
  public void writeNormalizerStats(ProcessingStats stats) throws IOException {
    Path outputPath = outputDirectory.resolve("normalizer_stats.json");
    JsonObject root = createNormalizerJson(stats);
    writeJsonToFile(root, outputPath);
    log.info("Written normalizer stats to {}", outputPath);
  }

  /**
   * Create a JSON object containing comparison statistics.
   *
   * @param stats The statistics to convert to JSON
   * @return JsonObject containing the statistics
   */
  private JsonObject createComparisonJson(ProcessingStats stats) {
    JsonObject root = new JsonObject();

    // Add basic statistics
    root.addProperty("totalProcessedClasses", stats.getTotalProcessedClasses());
    root.addProperty("identicalClasses", stats.getIdenticalClasses());
    root.addProperty("differentClasses", stats.getDifferentClasses().size());
    root.addProperty("disjunctionClasses", stats.getDisjunctionClasses().size());
    root.addProperty("failedClasses", stats.getErrorClasses());
    root.addProperty("errorPercentage", stats.getErrorPercentage());
    root.addProperty("similarityPercentage", stats.getSimilarityPercentage());

    // Add different classes details
    JsonArray differentClassesArray = new JsonArray();
    for (String className : stats.getDifferentClasses()) {
      JsonObject classDiff = new JsonObject();
      classDiff.addProperty("className", className);
      JsonArray appliedNormalizers = new JsonArray();
      stats.getAppliedNormalizers(className).forEach(appliedNormalizers::add);
      classDiff.add("appliedNormalizers", appliedNormalizers);
      differentClassesArray.add(classDiff);
    }
    root.add("differentClasses", differentClassesArray);

    // Add disjunction classes
    JsonArray disjunctionClassesArray = new JsonArray();
    stats.getDisjunctionClasses().forEach(disjunctionClassesArray::add);
    root.add("disjunctionClasses", disjunctionClassesArray);

    // Add errors
    JsonObject errors = new JsonObject();
    for (Map.Entry<String, ProcessingError> errorEntry : stats.getErrors().entrySet()) {
      JsonObject error = new JsonObject();
      ProcessingError pe = errorEntry.getValue();
      error.addProperty("className", pe.getClassName());
      error.addProperty("phase", pe.getPhase().toString());
      error.addProperty("errorType", pe.getErrorType());
      error.addProperty("errorMessage", pe.getErrorMessage());
      if (pe.getStackTrace() != null) {
        error.addProperty("stackTrace", pe.getStackTrace());
      }
      errors.add(errorEntry.getKey(), error);
    }
    root.add("errors", errors);

    return root;
  }

  /**
   * Create a JSON object containing normalizer statistics.
   *
   * @param stats The statistics to convert to JSON
   * @return JsonObject containing the statistics
   */
  private JsonObject createNormalizerJson(ProcessingStats stats) {
    JsonObject root = new JsonObject();
    JsonObject normalizerStats = new JsonObject();

    for (Map.Entry<String, NormalizerStats> entry : stats.getNormalizerStats().entrySet()) {
      String key = entry.getKey();
      String simpleName = key.substring(key.lastIndexOf('.') + 1); // Get simple name
      normalizerStats.addProperty(simpleName, entry.getValue().getCount());
    }

    root.add("normalizerStats", normalizerStats);
    return root;
  }

  /**
   * Write project-specific statistics to a JSON file.
   *
   * @param stats The statistics to write
   * @param outputPath The path to write the JSON file to
   * @throws IOException if there is an error writing the file
   */
  public void writeToJson(ProcessingStats stats, Path outputPath) throws IOException {
    JsonObject root = createComparisonJson(stats);
    writeJsonToFile(root, outputPath);
    log.info("Written project stats to {}", outputPath);
  }

  /**
   * Write aggregated statistics across multiple comparisons.
   *
   * @param aggregatedStats The aggregated statistics manager
   * @throws IOException if there is an error writing the file
   */
  public void writeAggregatedStats(AggregatedStatsManager aggregatedStats) throws IOException {
    Path outputPath = outputDirectory.resolve("aggregated_stats.json");
    writeJsonToFile(aggregatedStats.toJson(), outputPath);
    log.info("Written aggregated stats to {}", outputPath);
  }
}
