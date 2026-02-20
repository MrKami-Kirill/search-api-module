package ru.tecius.telemed.common.criteria;

import jakarta.persistence.criteria.Path;
import java.util.List;

/**
 * Record to hold Path, transformed values, and field type for Criteria API predicate building.
 * Used as a parameter for criteriaPredicateFunction in Operator enum.
 */
public record PathWithValue(Path<?> path, List<String> values, Class<?> fieldType) {
}
