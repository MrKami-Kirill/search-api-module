package ru.tecius.telemed.processor.validator;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.getField;
import static org.springframework.util.ReflectionUtils.makeAccessible;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.NonNull;

/**
 * Валидатор для аннотации {@link NotNullWhen}. Проверяет, что указанное поле не равно null, когда
 * выполняется условие.
 */
@RequiredArgsConstructor
public class NotNullWhenValidator implements ConstraintValidator<NotNullWhen, Object> {

  private String fieldName;
  private Expression expression;
  private EvaluationContext evalContext;

  /**
   * Инициализирует валидатор параметрами из аннотации.
   *
   * @param constraintAnnotation аннотация с параметрами валидации
   */
  public void initialize(NotNullWhen constraintAnnotation) {
    this.fieldName = constraintAnnotation.fieldName();
    var parser = new SpelExpressionParser();
    this.expression = parser.parseExpression(constraintAnnotation.expression());
    this.evalContext = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods()
        .build();
  }

  /**
   * Проверяет валидность объекта согласно условиям аннотации.
   *
   * @param value   проверяемый объект
   * @param context контекст валидации
   * @return true если объект валиден, иначе false
   */
  public boolean isValid(@NonNull Object value, ConstraintValidatorContext context) {
    var evalResult = ofNullable(expression.getValue(evalContext, value, Boolean.TYPE))
        .orElse(false);
    if (evalResult) {
      var path = fieldName.split("\\.");
      var curValue = value;
      for (var curFieldName : path) {
        var curField = findField(curValue.getClass(), curFieldName);
        if (isNull(curField)) {
          return false;
        }

        makeAccessible(curField);
        curValue = getField(curField, curValue);
        if (isNull(curValue)) {
          return false;
        }
      }
    }

    return true;
  }

}
