package ru.tecius.telemed.common.criteria;

import java.util.Optional;
import java.util.Set;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;

public interface CriteriaInfoInterface<E> {

  Class<E> getEntityClass();

  Set<CriteriaSearchAttribute> getCriteriaAttributes();

  Optional<CriteriaSearchAttribute> getCriteriaAttributeByJsonField(String jsonField);
}
