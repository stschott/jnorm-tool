package de.upb.sse.jnorm.core.exceptions;

/**
 * Exception thrown when errors occur during the normalization of Java code.
 *
 * <p>This exception is used to indicate problems that occur during:
 *
 * <ul>
 *   <li>Code structure analysis
 *   <li>AST transformations
 *   <li>Control flow normalization
 *   <li>Trap (exception handling) normalization
 * </ul>
 *
 * <p>Normalization exceptions typically represent issues that:
 *
 * <ul>
 *   <li>Are specific to the code normalization process
 *   <li>May indicate unsupported code patterns
 *   <li>Could be caused by complex or unusual code structures
 *   <li>Might require changes to the normalization strategy
 * </ul>
 *
 * <p>Example scenarios:
 *
 * <pre>
 * // Unsupported code pattern
 * throw new NormalizationException("Cannot normalize nested try-finally blocks");
 *
 * // Control flow error
 * throw new NormalizationException("Invalid control flow graph state");
 *
 * // Transformation failure
 * throw new NormalizationException("Failed to normalize method: " + methodName, cause);
 * </pre>
 *
 * @see TrapNormalizer
 * @see ControlFlowNormalizer
 */
public class NormalizationException extends JNormException {

  /** Constructs a new normalization exception with {@code null} as its detail message. */
  public NormalizationException() {
    super();
  }

  /**
   * Constructs a new normalization exception with the specified detail message.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method)
   */
  public NormalizationException(String message) {
    super(message);
  }

  /**
   * Constructs a new normalization exception with the specified detail message and cause.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method)
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  public NormalizationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new normalization exception with the specified cause and a detail message of
   * {@code (cause==null ? null : cause.toString())} (which typically contains the class and detail
   * message of {@code cause}).
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  public NormalizationException(Throwable cause) {
    super(cause);
  }
}
