package ru.tecius.telemed.common;

import java.util.Optional;
import java.util.Set;
import ru.tecius.telemed.configuration.MultipleSearchAttribute;
import ru.tecius.telemed.configuration.SimpleSearchAttribute;

public interface SearchInfoInterface<E> {

  String getSchemaName();

  String getTablaName();

  String getTableAlias();

  String getFullTableName();

  Set<SimpleSearchAttribute> getSimpleAttributes();

  Optional<SimpleSearchAttribute> getSimpleAttributeByJsonField(String jsonField);

  Set<MultipleSearchAttribute> getMultipleAttributes();

  Optional<MultipleSearchAttribute> getMultipleAttributeByJsonField(String jsonField);

}
