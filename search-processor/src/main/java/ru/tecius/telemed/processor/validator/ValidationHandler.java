package ru.tecius.telemed.processor.validator;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.tecius.telemed.processor.error.ErrorHandler;
import ru.tecius.telemed.processor.exception.ValidationException;

/**
 * Обработчик валидации DTO объектов. Проверяет валидность объектов используя Spring и Jakarta
 * валидацию.
 *
 * @param <T> тип валидируемого объекта
 */
public class ValidationHandler<T> {

  public static final String BODY_IS_NULL = "Body is null";
  public static final String COMMA_DELIMITER = ", ";

  private final Validator validator;
  private final jakarta.validation.Validator jakartaValidator;
  private final ErrorHandler errorHandler;

  /**
   * Создаёт обработчик валидации.
   *
   * @param validator валидатор Spring
   */
  public ValidationHandler(Validator validator, ErrorHandler errorHandler) {
    this.validator = validator;
    this.errorHandler = errorHandler;
    try (var factory = buildDefaultValidatorFactory()) {
      this.jakartaValidator = factory.getValidator();
    }
  }

  /**
   * Валидирует объект.
   *
   * @param body валидируемый объект
   * @return валидный объект
   * @throws ValidationException если объект не валиден
   */
  public final T validate(T body) {
    checkBodyIsNull(body);
    var errors = getErrors(body);
    if (isNotEmpty(errors.getAllErrors())) {
      var errorMessage = getErrorMessage(errors);
      errorHandler.reportError(errorMessage);
      throw new ValidationException(errorMessage);
    }

    return body;
  }

  /**
   * Валидирует объект с указанием групп валидации.
   *
   * @param body   валидируемый объект
   * @param groups группы валидации
   * @return валидный объект
   * @throws ValidationException если объект не валиден
   */
  public final T validate(T body, Class<?>... groups) {
    checkBodyIsNull(body);
    var errors = getErrors(body);
    var violations = getViolations(body, groups);
    if (isNotEmpty(errors.getAllErrors()) || isNotEmpty(violations)) {
      var errorMessage = getErrorMessage(errors, violations);
      errorHandler.reportError(errorMessage);
      throw new ValidationException(errorMessage);
    }

    return body;
  }

  private void checkBodyIsNull(T body) {
    if (isNull(body)) {
      errorHandler.reportError(BODY_IS_NULL);
      throw new ValidationException(BODY_IS_NULL);
    }
  }

  private Errors getErrors(T body) {
    var errors = new BeanPropertyBindingResult(body, body.getClass().getName());
    validator.validate(body, errors);
    return errors;
  }

  private Set<ConstraintViolation<T>> getViolations(T body, Class<?>... groups) {
    return jakartaValidator.validate(body, groups);
  }

  private String getErrorMessage(Errors errors, Set<ConstraintViolation<T>> violations) {
    return join(COMMA_DELIMITER,
        Stream.of(getErrorMessage(errors), violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(joining(COMMA_DELIMITER)))
            .filter(StringUtils::isNotBlank)
            .toList());
  }

  private String getErrorMessage(Errors errors) {
    return errors.getAllErrors().stream()
        .map(DefaultMessageSourceResolvable::getDefaultMessage)
        .collect(joining(COMMA_DELIMITER));
  }

}
