package ru.tecius.telemed.exception;

/**
 * Исключение при ошибке валидации.
 */
public class ValidationException extends RuntimeException {

  /**
   * Создаёт исключение с сообщением.
   *
   * @param message сообщение об ошибке
   */
  public ValidationException(String message) {
    super(message);
  }

  /**
   * Создаёт исключение с сообщением и причиной.
   *
   * @param message сообщение об ошибке
   * @param cause причина ошибки
   */
  public ValidationException(String message, Throwable cause) {
    super(message, cause);
  }

}
