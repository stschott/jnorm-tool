package de.upb.sse.jnorm.core.config;

import java.nio.file.Path;
import lombok.Getter;

/** Configuration settings for benchmark execution and reporting. */
@Getter
public class BenchmarkConfig {
  private final Path outputDirectory;
  private final Path statsDirectory;
  private final boolean generateDetailedStats;
  private final String sourceVersion;
  private final String targetVersion;
  private final boolean compareNormalizedOnly;

  private BenchmarkConfig(Builder builder) {
    this.outputDirectory = builder.outputDirectory;
    this.statsDirectory = builder.statsDirectory;
    this.generateDetailedStats = builder.generateDetailedStats;
    this.compareNormalizedOnly = builder.compareNormalizedOnly;
    this.sourceVersion = builder.sourceVersion;
    this.targetVersion = builder.targetVersion;
  }

  public static class Builder {
    private Path outputDirectory;
    private Path statsDirectory;
    private boolean generateDetailedStats;
    private boolean compareNormalizedOnly;
    private String sourceVersion;
    private String targetVersion;

    public Builder setOutputDirectory(Path outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    public Builder setStatsDirectory(Path statsDirectory) {
      this.statsDirectory = statsDirectory;
      return this;
    }

    public Builder setGenerateDetailedStats(boolean generateDetailedStats) {
      this.generateDetailedStats = generateDetailedStats;
      return this;
    }

    public Builder setCompareNormalizedOnly(boolean compareNormalizedOnly) {
      this.compareNormalizedOnly = compareNormalizedOnly;
      return this;
    }

    public Builder setSourceVersion(String sourceVersion) {
      this.sourceVersion = sourceVersion;
      return this;
    }

    public Builder setTargetVersion(String targetVersion) {
      this.targetVersion = targetVersion;
      return this;
    }

    public BenchmarkConfig build() {
      return new BenchmarkConfig(this);
    }
  }
}
