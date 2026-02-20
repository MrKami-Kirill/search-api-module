package ru.tecius.telemed.common.criteria;

import java.util.Optional;
import java.util.Set;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;

public interface CriteriaInfoInterface<E> {

  Class<E> getEntityClass();

  Set<CriteriaSearchAttribute> getSimpleAttributes();

  Optional<CriteriaSearchAttribute> getSimpleAttributeByJsonKey(String key);

  Set<CriteriaSearchAttribute> getMultipleAttributes();

  Optional<CriteriaSearchAttribute> getMultipleAttributeByJsonKey(String key);
}
