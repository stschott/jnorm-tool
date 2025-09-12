package de.upb.sse.jnorm.core.config;

public class ProcessingConfig {
  private final boolean applyOptimizations;
  private final boolean applyNormalization;
  private final boolean applyAggressiveNormalization;

  private ProcessingConfig(Builder builder) {
    this.applyOptimizations = builder.applyOptimizations;
    this.applyNormalization = builder.applyNormalization;
    this.applyAggressiveNormalization = builder.applyAggressiveNormalization;
  }

  public boolean isOptimizationEnabled() {
    return applyOptimizations;
  }

  public boolean isNormalizationEnabled() {
    return applyNormalization;
  }

  public boolean isAggressiveNormalizationEnabled() {
    return applyAggressiveNormalization;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean applyOptimizations = true;
    private boolean applyNormalization = true;
    private boolean applyAggressiveNormalization = true;

    public Builder applyOptimizations(boolean applyOptimizations) {
      this.applyOptimizations = applyOptimizations;
      return this;
    }

    public Builder applyNormalization(boolean applyNormalization) {
      this.applyNormalization = applyNormalization;
      return this;
    }

    public Builder applyAggressiveNormalization(boolean applyAggressiveNormalization) {
      this.applyAggressiveNormalization = applyAggressiveNormalization;
      return this;
    }

    public ProcessingConfig build() {
      return new ProcessingConfig(this);
    }
  }
}
