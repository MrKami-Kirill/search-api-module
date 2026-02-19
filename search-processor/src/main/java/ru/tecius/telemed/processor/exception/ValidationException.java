package ru.tecius.telemed.processor.exception;

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

}
