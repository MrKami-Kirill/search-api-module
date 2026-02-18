package ru.tecius.telemed.dto.request;

import java.util.Arrays;
import java.util.function.Function;

public enum Operator {

  EQUAL("Совпадает", "=", value -> value, "?%s", true),
  NOT_EQUAL("Не совпадает", "!=", value -> value, "?%s", true),
  IN("Содержит", "IN", value -> Arrays.asList(value.split(",")), "(?%s)", true),
  CONTAIN("Содержит", "LIKE", value -> String.format("%%%s%%", value), "?%s", true),
  EXCLUDE("", "NOT LIKE", value -> String.format("%%%s%%", value), "?%s", true),
  EXCLUDE_DISABLE_ELASTIC_ESCAPE("", "NOT LIKE", value -> String.format("%%%s%%", value), "?%s",
      true),
  BEGIN("", "LIKE", value -> String.format("%s%%", value), "?%s", true),
  NOT_BEGIN("", "NOT LIKE", value -> String.format("%s%%", value), "?%s", true),
  END("", "LIKE", value -> String.format("%%%s", value), "?%s", true),
  NOT_END("", "NOT LIKE", value -> String.format("%%%s", value), "?%s", true),
  IS_EMPTY("", "IS EMPTY", value -> "", "", false),
  IS_NULL("", "IS NULL", value -> "", "", false),
  IS_NOT_NULL("", "IS NOT NULL", value -> "", "", false),
  OR("ИЛИ", "OR", value -> "", "", false),
  AND("И", "AND", value -> "", "", false),
  BETWEEN("", "BETWEEN", value -> value, "?%s", false),
  MORE_OR_EQUAL("Больше или равно", ">=", value -> value, "?%s", false),
  LESS_OR_EQUAL("Меньше или равно", "<=", value -> value, "?%s", false),
  EXISTS_EQUAL_VALUE_OR_NULL("", "EXISTS (SELECT 1 FROM %s %s WHERE ", value -> value, "?%s", true),
  EXIST_IN("", "EXISTS (SELECT 1 FROM %s %s WHERE ", value -> Arrays.asList(value.split(",")),
      "(?%s)", true),
  NOT_EXISTS("", "NOT EXISTS(SELECT 1 FROM %s %s WHERE ", value -> "", "", true);

  private final String rusName;
  private final String sqlOperator;
  private final Function<String, Object> jpaValuePalceholderFunction;
  private final String jpaValuePlaceholderType;
  private final Boolean allowsCaseInsensitiveSearch;

  Operator(String rusName,
      String sqlOperator,
      Function<String, Object> jpaValuePalceholderFunction,
      String jpaValuePlaceholderType, Boolean allowsCaseInsensitiveSearch) {
    this.rusName = rusName;
    this.sqlOperator = sqlOperator;
    this.jpaValuePalceholderFunction = jpaValuePalceholderFunction;
    this.jpaValuePlaceholderType = jpaValuePlaceholderType;
    this.allowsCaseInsensitiveSearch = allowsCaseInsensitiveSearch;
  }

  public String getRusName() {
    return rusName;
  }

  public String getSqlOperator() {
    return sqlOperator;
  }

  public Function<String, Object> getJpaValuePalceholderFunction() {
    return jpaValuePalceholderFunction;
  }

  public String getJpaValuePlaceholderType() {
    return jpaValuePlaceholderType;
  }

  public Boolean getAllowsCaseInsensitiveSearch() {
    return allowsCaseInsensitiveSearch;
  }
}
