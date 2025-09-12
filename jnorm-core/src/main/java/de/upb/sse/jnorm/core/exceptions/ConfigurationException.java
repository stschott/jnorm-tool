package de.upb.sse.jnorm.core.exceptions;

/**
 * Exception thrown when there is an error in the configuration or setup of JNorm components.
 *
 * <p>This exception is used to indicate problems with:
 *
 * <ul>
 *   <li>Invalid configuration parameters
 *   <li>Missing required configuration
 *   <li>Incompatible configuration combinations
 *   <li>Component initialization failures
 * </ul>
 *
 * <p>Configuration exceptions typically represent issues that:
 *
 * <ul>
 *   <li>Can be detected early, often at startup or during component initialization
 *   <li>Are related to how JNorm is configured rather than how it processes code
 *   <li>Should be addressed by modifying the configuration rather than the input code
 * </ul>
 *
 * <p>Example scenarios:
 *
 * <pre>
 * // Invalid configuration
 * throw new ConfigurationException("Optimization cannot be enabled without normalization");
 *
 * // Missing required component
 * throw new ConfigurationException("No class normalizers configured");
 *
 * // Initialization failure
 * throw new ConfigurationException("Failed to initialize SootUpWrapper", cause);
 * </pre>
 *
 * @see ProcessingConfig
 * @see SourceProcessorFactory
 */
public class ConfigurationException extends JNormException {

  /** Constructs a new configuration exception with {@code null} as its detail message. */
  public ConfigurationException() {
    super();
  }

  /**
   * Constructs a new configuration exception with the specified detail message.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method)
   */
  public ConfigurationException(String message) {
    super(message);
  }

  /**
   * Constructs a new configuration exception with the specified detail message and cause.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *     #getMessage()} method)
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new configuration exception with the specified cause and a detail message of
   * {@code (cause==null ? null : cause.toString())} (which typically contains the class and detail
   * message of {@code cause}).
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *     A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
   */
  public ConfigurationException(Throwable cause) {
    super(cause);
  }
}
