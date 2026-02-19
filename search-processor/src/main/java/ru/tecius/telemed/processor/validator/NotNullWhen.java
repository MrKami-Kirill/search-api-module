package ru.tecius.telemed.processor.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для условной валидации. Позволяет проверить, что поле не равно null, когда выполняется
 * указанное условие.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Constraint(
    validatedBy = {NotNullWhenValidator.class}
)
@Repeatable(NotNullWhen.List.class)
public @interface NotNullWhen {

  /**
   * Сообщение об ошибке валидации.
   *
   * @return сообщение об ошибке
   */
  String message() default "When {expression} field {fieldName} must not be null.";

  /**
   * Группы валидации.
   *
   * @return группы валидации
   */
  Class<?>[] groups() default {};

  /**
   * Метаданные для payload.
   *
   * @return payload
   */
  Class<? extends Payload>[] payload() default {};

  /**
   * Имя поля для валидации.
   *
   * @return имя поля
   */
  String fieldName();

  /**
   * Условное выражение для проверки.
   *
   * @return выражение
   */
  String expression();

  /**
   * Контейнер для повторяющихся аннотаций.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE})
  @interface List {

    /**
     * Массив аннотаций.
     *
     * @return массив аннотаций
     */
    NotNullWhen[] value();
  }
}
