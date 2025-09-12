package de.upb.sse.jnorm.core.stats;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StatisticsReporterTest {
  @TempDir Path tempDir;

  private StatisticsCollector collector;
  private StatisticsReporter reporter;

  @BeforeEach
  void setUp() {
    collector = new DefaultStatisticsCollector();
    reporter = new StatisticsReporter(tempDir);
  }

  @Test
  void shouldWriteNormalizerStats() throws IOException {
    // Given
    collector.recordNormalization(
        "Test1", "jnorm.core.normalization.methodnormalizers.TrapNormalizer", true, null);
    collector.recordNormalization(
        "Test2", "jnorm.core.normalization.methodnormalizers.ControlFlowNormalizer", true, null);

    // When
    reporter.writeNormalizerStats(collector.getStatistics());

    // Then
    Path statsFile = tempDir.resolve("normalizer_stats.json");
    assertTrue(Files.exists(statsFile));

    JsonObject parsedJson = new Gson().fromJson(Files.readString(statsFile), JsonObject.class);
    JsonObject normStats = parsedJson.getAsJsonObject("normalizerStats");

    assertEquals(1, normStats.get("TrapNormalizer").getAsInt());
    assertEquals(1, normStats.get("ControlFlowNormalizer").getAsInt());
  }

  @Test
  void shouldWriteComparisonStats() throws IOException {
    // Given
    collector.recordNormalization("Test1", "TrapNormalizer", true, null);
    collector.recordProcessing("com.example.Test", true, null);
    collector.recordProcessing("com.example.Failed", false, "Test error");

    // When
    reporter.writeComparisonStats(
        "testProject", "normalized", "v1", "v2", collector.getStatistics());

    // Then
    Path statsFile = tempDir.resolve("testProject_normalized_v1_vs_v2.json");
    assertTrue(Files.exists(statsFile));

    JsonObject parsedJson = new Gson().fromJson(Files.readString(statsFile), JsonObject.class);

    // Check basic stats
    assertEquals(2, parsedJson.get("totalProcessedClasses").getAsInt());
    assertEquals(1, parsedJson.get("identicalClasses").getAsInt());
    assertEquals(1, parsedJson.get("differentClasses").getAsJsonArray().size());
    assertEquals(1, parsedJson.get("failedClasses").getAsInt());

    // Check different classes details
    JsonArray differentClasses = parsedJson.getAsJsonArray("differentClasses");
    JsonObject diffClass = differentClasses.get(0).getAsJsonObject();
    assertEquals("com.example.Failed", diffClass.get("className").getAsString());
    assertTrue(diffClass.has("appliedNormalizers"));

    // Check errors
    JsonObject errors = parsedJson.getAsJsonObject("errors");
    assertTrue(errors.has("com.example.Failed"));
    JsonObject error = errors.get("com.example.Failed").getAsJsonObject();
    assertEquals("Test error", error.get("errorMessage").getAsString());
  }

  @Test
  void shouldWriteComprehensiveStats() throws IOException {
    // Given
    collector.recordNormalization("Test1", "TrapNormalizer", true, null);
    collector.recordProcessing("com.example.Test", true, null);
    collector.recordProcessing("com.example.Failed", false, "Error message");

    // When
    Path statsFile = tempDir.resolve("stats.json");
    reporter.writeToJson(collector.getStatistics(), statsFile);

    // Then
    assertTrue(Files.exists(statsFile));
    JsonObject parsedJson = new Gson().fromJson(Files.readString(statsFile), JsonObject.class);

    // Check basic stats
    assertEquals(2, parsedJson.get("totalProcessedClasses").getAsInt());
    assertEquals(1, parsedJson.get("identicalClasses").getAsInt());
    assertEquals(1, parsedJson.get("differentClasses").getAsJsonArray().size());
    assertEquals(1, parsedJson.get("failedClasses").getAsInt());

    // Check error details
    JsonObject errorDetails = parsedJson.getAsJsonObject("errors");
    assertTrue(errorDetails.has("com.example.Failed"));
    assertEquals(
        "Error message",
        errorDetails.getAsJsonObject("com.example.Failed").get("errorMessage").getAsString());
  }
}
