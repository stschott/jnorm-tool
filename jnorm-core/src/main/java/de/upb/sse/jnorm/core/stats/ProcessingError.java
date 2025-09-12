package de.upb.sse.jnorm.core.stats;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Represents an error that occurred during class processing. This could be a framework error
 * (SootUp) or a normalization error.
 */
@Getter
public class ProcessingError {
  public enum ErrorPhase {
    FRAMEWORK_LOADING, // When SootUp fails to load/parse the class
    FRAMEWORK_PROCESSING, // Other SootUp-related errors
    NORMALIZATION, // Our normalization errors
    COMPARISON // Errors during comparison
  }

  private final String className;
  private final ErrorPhase phase;
  private final String errorType;
  private final String errorMessage;
  private final String stackTrace;
  private final Map<String, String> context;

  public ProcessingError(
      String className,
      ErrorPhase phase,
      String errorType,
      String errorMessage,
      String stackTrace) {
    this.className = className;
    this.phase = phase;
    this.errorType = errorType;
    this.errorMessage = errorMessage;
    this.stackTrace = stackTrace;
    this.context = new HashMap<>();
  }

  public ProcessingError(String errorMessage) {
    this("", ErrorPhase.NORMALIZATION, "NormalizationError", errorMessage, "");
  }
}
