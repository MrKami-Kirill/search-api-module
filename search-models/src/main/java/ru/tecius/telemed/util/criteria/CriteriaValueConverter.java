package ru.tecius.telemed.util.criteria;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import ru.tecius.telemed.exception.ValidationException;

/**
 * Utility class for converting values to appropriate types for JPA Criteria API predicates.
 * Extracted from AbstractCriteriaSqlService to be reusable by Operator enum.
 */
public final class CriteriaValueConverter {

  private CriteriaValueConverter() {
    // Utility class - prevent instantiation
  }

  /**
   * Converts a string value to the appropriate type based on field type.
   *
   * @param value     the string value to convert
   * @param fieldType the target field type
   * @return the converted value
   */
  public static Object convertValue(String value, Class<?> fieldType) {
    if (value == null) {
      return null;
    }

    return switch (fieldType) {
      case Class<?> c when c == Long.class -> Long.parseLong(value);
      case Class<?> c when c == Integer.class -> Integer.parseInt(value);
      case Class<?> c when c == Double.class -> Double.parseDouble(value);
      case Class<?> c when c == Float.class -> Float.parseFloat(value);
      case Class<?> c when c == Boolean.class -> Boolean.parseBoolean(value);
      default -> value;
    };
  }

  /**
   * Converts a list of string values to an array of appropriate types based on field type.
   *
   * @param values    the list of string values to convert
   * @param fieldType the target field type
   * @return array of converted values
   */
  public static Object[] convertValues(List<String> values, Class<?> fieldType) {
    return values.stream()
        .map(v -> convertValue(v, fieldType))
        .toArray();
  }

  /**
   * Parses a date string value into the appropriate date type.
   *
   * @param value     the string value to parse
   * @param fieldType the target date type (OffsetDateTime, LocalDateTime, or LocalDate)
   * @return the parsed Comparable date value
   * @throws ValidationException if parsing fails
   */
  @SuppressWarnings("unchecked,rawtypes")
  public static Comparable parseDateValue(String value, Class<?> fieldType) {
    try {
      return switch (fieldType) {
        case Class<?> c when c == OffsetDateTime.class -> {
          // Try to parse date with timezone
          try {
            // First try standard ISO 8601 format
            yield OffsetDateTime.parse(value);
          } catch (Exception e) {
            // If that fails, try format with colon in timezone
            var formatter = ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            // Replace colon in timezone for parsing
            var normalized = value.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
            yield OffsetDateTime.parse(normalized, formatter);
          }
        }
        case Class<?> c when c == LocalDateTime.class -> LocalDateTime.parse(value,
            ISO_LOCAL_DATE_TIME);
        case Class<?> c when c == LocalDate.class -> LocalDate.parse(value, ISO_LOCAL_DATE);
        default -> throw new IllegalArgumentException("Unsupported date type: " + fieldType);
      };
    } catch (Exception e) {
      throw new ValidationException(
          "Ошибка парсинга даты: %s для типа %s".formatted(value, fieldType), e);
    }
  }
}
