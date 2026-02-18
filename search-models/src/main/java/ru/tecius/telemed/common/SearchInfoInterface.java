package ru.tecius.telemed.common;

import java.util.Set;
import ru.tecius.telemed.configuration.MultipleSearchAttribute;
import ru.tecius.telemed.configuration.SimpleSearchAttribute;

public interface SearchInfoInterface<E> {

  String getSchemaName();

  String getTablaName();

  String getTableAlias();

  Set<SimpleSearchAttribute> getSimpleAttributes();

  Set<MultipleSearchAttribute> getMultipleAttributes();

}
