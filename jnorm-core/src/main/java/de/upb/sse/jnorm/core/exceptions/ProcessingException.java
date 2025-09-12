package de.upb.sse.jnorm.core.exceptions;

/**
 * Exception thrown when errors occur during the processing of source code.
 *
 * <p>This exception is used to indicate problems that occur during:
 *
 * <ul>
 *   <li>Source file reading and parsing
 *   <li>View creation from source files
 *   <li>Class loading and analysis
 *   <li>Code transformation operations
 * </ul>
 *
 * <p>Processing exceptions typically represent issues that:
 *
 * <ul>
 *   <li>Occur during the actual processing of source code
 *   <li>May be caused by invalid or malformed input code
 *   <li>Could be temporary failures (e.g., I/O errors)
 *   <li>Might require changes to the input code or environment
 * </ul>
 *
 * <p>Example scenarios:
 *
 * <pre>
 * // File reading error
 * throw new ProcessingException("Failed to read source file: " + file, ioException);
 *
 * // View creation failure
 * throw new ProcessingException("Failed to create view from directory: " + dir);
 *
 * // Class loading error
 * throw new ProcessingException("Could not load class: " + className, classNotFoundException);
 * </pre>
 *
 * @see SourceProcessor
 * @see SootUpWrapper
 */
public class ProcessingException extends JNormException {

  /** Constructs a new processing exception with {@code null} as its detail message. */
  public ProcessingException() {
    super();
  }

  /**
   * Constructs a new processing exception with the specified detail message.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method)
   */
  public ProcessingException(String message) {
    super(message);
  }

  /**
   * Constructs a new processing exception with the specified detail message and cause.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method)
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  public ProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new processing exception with the specified cause and a detail message of {@code
   * (cause==null ? null : cause.toString())} (which typically contains the class and detail message
   * of {@code cause}).
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  public ProcessingException(Throwable cause) {
    super(cause);
  }
}
