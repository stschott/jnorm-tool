package de.upb.sse.jnorm.core.exceptions;

/**
 * Base exception class for all JNorm-specific exceptions. This class serves as the root of the
 * JNorm exception hierarchy, providing a way to distinguish JNorm-specific exceptions from other
 * runtime exceptions.
 *
 * <p>The JNorm exception hierarchy includes:
 *
 * <ul>
 *   <li>{@link ConfigurationException} - For configuration and setup errors
 *   <li>{@link ProcessingException} - For errors during source code processing
 *   <li>{@link NormalizationException} - For errors during code normalization
 * </ul>
 *
 * <p>All JNorm exceptions are unchecked exceptions, extending {@link RuntimeException}.
 *
 * @see ConfigurationException
 * @see ProcessingException
 * @see NormalizationException
 */
public class JNormException extends RuntimeException {

  /** Constructs a new JNorm exception with {@code null} as its detail message. */
  public JNormException() {
    super();
  }

  /**
   * Constructs a new JNorm exception with the specified detail message.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method)
   */
  public JNormException(String message) {
    super(message);
  }

  /**
   * Constructs a new JNorm exception with the specified detail message and cause.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method)
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  public JNormException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new JNorm exception with the specified cause and a detail message of {@code
   * (cause==null ? null : cause.toString())} (which typically contains the class and detail message
   * of {@code cause}).
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  public JNormException(Throwable cause) {
    super(cause);
  }
}
