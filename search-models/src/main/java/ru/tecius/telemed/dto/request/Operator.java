package ru.tecius.telemed.dto.request;

import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import ru.tecius.telemed.exception.ValidationException;

@RequiredArgsConstructor
@Getter
public enum Operator {

  EQUAL("=", value -> getFirstElement(value, "EQUAL")),
  NOT_EQUAL("!=", value -> getFirstElement(value, "NOT_EQUAL")),
  IN("IN",
      value -> "(%s)".formatted(join(",", value.stream()
          .map(""::formatted)
          .toList()))),
  CONTAIN("LIKE",
      value -> "%%%s%%".formatted(getFirstElement(value, "CONTAIN"))),
  EXCLUDE("NOT LIKE",
      value -> "%%%s%%".formatted(getFirstElement(value, "EXCLUDE"))),
  BEGIN("LIKE",
      value -> "%s%%".formatted(getFirstElement(value, "BEGIN"))),
  NOT_BEGIN("NOT LIKE",
      value -> "%s%%".formatted(getFirstElement(value, "NOT_BEGIN"))),
  END("LIKE",
      value -> "%%%s".formatted(getFirstElement(value, "END"))),
  NOT_END("NOT LIKE",
      value -> "%%%s".formatted(getFirstElement(value, "NOT_END"))),
  IS_EMPTY("IS EMPTY", value -> EMPTY),
  IS_NULL("IS NULL", value -> EMPTY),
  IS_NOT_NULL("IS NOT NULL", value -> EMPTY),
  OR("OR", value -> EMPTY),
  AND("AND", value -> EMPTY),
  BETWEEN("BETWEEN",
      value -> {
        var pair = getFirstAndLastElements(value, "BETWEEN");

      }),
  MORE_OR_EQUAL(">=", value -> value),
  LESS_OR_EQUAL("<=", value -> value),
  EXISTS_EQUAL_VALUE_OR_NULL("EXISTS (SELECT 1 FROM %s %s WHERE ", value -> value),
  EXIST_IN("EXISTS (SELECT 1 FROM %s %s WHERE ", value -> Arrays.asList(value.split(","))),
  NOT_EXISTS("NOT EXISTS(SELECT 1 FROM %s %s WHERE ", value -> "");

  private final String sqlOperator;
  private final Function<List<String>, Object> valuePalceholderFunction;

  private static Function<List<String>, String> getFirstElement(List<String> value, String operator) {
    if (!Objects.equals(value.size(), 1)) {
      throw new ValidationException("Для оператора %s в value должно быть передано одно значение"
          .formatted(operator));
    }

    return value.getFirst();
  }

  private static Pair<String, String> getFirstAndLastElements(List<String> value, String operator) {
    if (!Objects.equals(value.size(), 2)) {
      throw new ValidationException("Для оператора %s в value должно быть передано два значения"
          .formatted(operator));
    }

    return Pair.of(value.getFirst(), value.getLast());
  }

}
