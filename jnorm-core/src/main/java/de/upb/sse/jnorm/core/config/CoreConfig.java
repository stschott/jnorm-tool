package de.upb.sse.jnorm.core.config;

import lombok.Getter;

/** Core configuration settings for JNorm processing. */
@Getter
public class CoreConfig {
  private final boolean optimizationEnabled;
  private final boolean normalizationEnabled;
  private final boolean aggressiveNormalizationEnabled;

  private CoreConfig(Builder builder) {
    this.optimizationEnabled = builder.optimizationEnabled;
    this.normalizationEnabled = builder.normalizationEnabled;
    this.aggressiveNormalizationEnabled = builder.aggressiveNormalizationEnabled;
  }

  public static class Builder {
    private boolean optimizationEnabled = true;
    private boolean normalizationEnabled = true;
    private boolean aggressiveNormalizationEnabled = false;

    public Builder setOptimizationEnabled(boolean enabled) {
      this.optimizationEnabled = enabled;
      return this;
    }

    public Builder setNormalizationEnabled(boolean enabled) {
      this.normalizationEnabled = enabled;
      return this;
    }

    public Builder setAggressiveNormalizationEnabled(boolean enabled) {
      this.aggressiveNormalizationEnabled = enabled;
      return this;
    }

    public CoreConfig build() {
      return new CoreConfig(this);
    }
  }
}
