package ru.tecius.telemed.dto.request;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.join;
import static ru.tecius.telemed.util.Constants.DATE_CLASSES;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import ru.tecius.telemed.exception.ValidationException;
import ru.tecius.telemed.util.criteria.CriteriaValueConverter;
import ru.tecius.telemed.util.nativ.NativeValueConverter;

@Getter
public enum Operator {

  EQUAL(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s = ?".formatted(field),
      NativeValueConverter::transformValues,
      (cb, pv) -> cb.equal(pv.path(),
          CriteriaValueConverter.convertValue(pv.values().getFirst(), pv.fieldType()))),

  NOT_EQUAL(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s <> ?".formatted(field),
      NativeValueConverter::transformValues,
      (cb, pv) -> cb.notEqual(pv.path(),
          CriteriaValueConverter.convertValue(pv.values().getFirst(), pv.fieldType()))),

  IN(
      CollectionUtils::isNotEmpty,
      (field, values) -> "%s IN (%s)".formatted(field,
          join(", ", nCopies(values.size(), "?"))),
      NativeValueConverter::transformValues,
      (cb, pv) -> pv.path().in(CriteriaValueConverter.convertValues(pv.values(),
          pv.fieldType()))),

  CONTAIN(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s LIKE ?".formatted(field),
      (values, fieldType) -> NativeValueConverter.transformValuesForLike(values),
      (cb, pv) -> cb.like(pv.path().as(String.class), pv.values().getFirst())),

  EXCLUDE(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      (values, fieldType) -> NativeValueConverter.transformValuesForLike(values),
      (cb, pv) -> cb.notLike(pv.path().as(String.class), pv.values().getFirst())),

  BEGIN(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s LIKE ?".formatted(field),
      (values, fieldType) -> NativeValueConverter.transformValuesForBegin(values),
      (cb, pv) -> cb.like(pv.path().as(String.class), pv.values().getFirst())),

  NOT_BEGIN(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      (values, fieldType) -> NativeValueConverter.transformValuesForBegin(values),
      (cb, pv) -> cb.notLike(pv.path().as(String.class), pv.values().getFirst())),

  END(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s LIKE ?".formatted(field),
      (values, fieldType) -> NativeValueConverter.transformValuesForEnd(values),
      (cb, pv) -> cb.like(pv.path().as(String.class), pv.values().getFirst())),

  NOT_END(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s NOT LIKE ?".formatted(field),
      (values, fieldType) -> NativeValueConverter.transformValuesForEnd(values),
      (cb, pv) -> cb.notLike(pv.path().as(String.class), pv.values().getFirst())),

  IS_NULL(
      CollectionUtils::isEmpty,
      (field, values) -> "%s IS NULL".formatted(field),
      (values, fieldType) -> emptyList(),
      (cb, pv) -> cb.isNull(pv.path())),

  IS_NOT_NULL(
      CollectionUtils::isEmpty,
      (field, values) -> "%s IS NOT NULL".formatted(field),
      (values, fieldType) -> emptyList(),
      (cb, pv) -> cb.isNotNull(pv.path())),

  BETWEEN(
      values -> isNotEmpty(values) && Objects.equals(2, values.size()),
      (field, values) -> "%s BETWEEN ? AND ?".formatted(field),
      NativeValueConverter::transformValues,
      Operator::buildBetweenPredicate),

  MORE_OR_EQUAL(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s >= ?".formatted(field),
      NativeValueConverter::transformValues,
      Operator::buildMoreOrEqualPredicate),

  LESS_OR_EQUAL(
      values -> isNotEmpty(values) && Objects.equals(1, values.size()),
      (field, values) -> "%s <= ?".formatted(field),
      NativeValueConverter::transformValues,
      Operator::buildLessOrEqualPredicate);

  private final java.util.function.Predicate<List<String>> valuePredicate;
  private final BiFunction<String, List<String>, String> nativeSqlTemplateFunction;
  private final BiFunction<List<String>, Class<?>, List<String>> nativeTransformValueFunction;
  private final BiFunction<CriteriaBuilder, PathWithValue, Object> criteriaPredicateFunction;

  Operator(
      java.util.function.Predicate<List<String>> valuePredicate,
      BiFunction<String, List<String>, String> nativeSqlTemplateFunction,
      BiFunction<List<String>, Class<?>, List<String>> nativeTransformValueFunction,
      BiFunction<CriteriaBuilder, PathWithValue, Object> criteriaPredicateFunction
  ) {
    this.valuePredicate = valuePredicate;
    this.nativeSqlTemplateFunction = nativeSqlTemplateFunction;
    this.nativeTransformValueFunction = nativeTransformValueFunction;
    this.criteriaPredicateFunction = criteriaPredicateFunction;
  }

  public void checkValue(List<String> values) {
    if (!valuePredicate.test(values)) {
      throw new ValidationException("Для оператора %s переданы некорректные value"
          .formatted(name()));
    }
  }

  public String buildNativeCondition(String dbField, List<String> values) {
    return nativeSqlTemplateFunction.apply(dbField, values);
  }

  @SuppressWarnings("unchecked,rawtypes")
  private static Predicate buildBetweenPredicate(CriteriaBuilder cb, PathWithValue pv) {
    if (Objects.equals(pv.fieldType(), String.class)) {
      return cb.between(pv.path().as(String.class), pv.values().getFirst(), pv.values().getLast());
    }

    if (DATE_CLASSES.contains(pv.fieldType())) {
      return cb.between(
          (Path) pv.path(),
          CriteriaValueConverter.parseDateValue(pv.values().getFirst(), pv.fieldType()),
          CriteriaValueConverter.parseDateValue(pv.values().getLast(), pv.fieldType())
      );
    }

    return cb.between(
        (Path) pv.path(),
        (Comparable) CriteriaValueConverter.convertValue(pv.values().getFirst(), pv.fieldType()),
        (Comparable) CriteriaValueConverter.convertValue(pv.values().getLast(), pv.fieldType())
    );
  }

  @SuppressWarnings("unchecked,rawtypes")
  private static Predicate buildMoreOrEqualPredicate(CriteriaBuilder cb, PathWithValue pv) {
    if (Objects.equals(pv.fieldType(), String.class)) {
      return cb.greaterThanOrEqualTo(pv.path().as(String.class), pv.values().getFirst());
    }

    if (DATE_CLASSES.contains(pv.fieldType())) {
      return cb.greaterThanOrEqualTo(
          (Path) pv.path(),
          CriteriaValueConverter.parseDateValue(pv.values().getFirst(), pv.fieldType())
      );
    }

    return cb.greaterThanOrEqualTo(
        (Path) pv.path(),
        (Comparable) CriteriaValueConverter.convertValue(pv.values().getFirst(), pv.fieldType())
    );
  }

  @SuppressWarnings("unchecked,rawtypes")
  private static Predicate buildLessOrEqualPredicate(CriteriaBuilder cb, PathWithValue pv) {
    if (Objects.equals(pv.fieldType(), String.class)) {
      return cb.lessThanOrEqualTo(pv.path().as(String.class), pv.values().getFirst());
    }

    if (DATE_CLASSES.contains(pv.fieldType())) {
      return cb.lessThanOrEqualTo(
          (Path) pv.path(),
          CriteriaValueConverter.parseDateValue(pv.values().getFirst(), pv.fieldType())
      );
    }

    return cb.lessThanOrEqualTo(
        (Path) pv.path(),
        (Comparable) CriteriaValueConverter.convertValue(pv.values().getFirst(), pv.fieldType())
    );
  }

}
