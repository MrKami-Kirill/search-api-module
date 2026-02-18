package ru.tecius.telemed.common;

import java.util.List;
import ru.tecius.telemed.configuration.MultipleSearchAttribute;
import ru.tecius.telemed.configuration.SimpleSearchAttribute;

public interface SearchInfoInterface<E> {

  String getSchemaName();

  String getTablaName();

  String getTableAlias();

  List<SimpleSearchAttribute> getSimpleAttributes();

  //List<MultipleSearchAttribute> getMultipleAttributes();

}
