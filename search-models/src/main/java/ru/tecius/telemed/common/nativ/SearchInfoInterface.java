package ru.tecius.telemed.common.nativ;

import java.util.Optional;
import java.util.Set;
import ru.tecius.telemed.configuration.nativ.JoinInfo;
import ru.tecius.telemed.configuration.nativ.NativeSearchAttribute;

public interface SearchInfoInterface<E> {

  String getSchemaName();

  String getTablaName();

  String getTableAlias();

  String getFullTableName();

  Set<NativeSearchAttribute> getSimpleAttributes();

  Optional<NativeSearchAttribute> getSimpleAttributeByJsonField(String jsonField);

  Set<NativeSearchAttribute> getMultipleAttributes();

  Optional<NativeSearchAttribute> getMultipleAttributeByJsonField(String jsonField);

  String createJoinString(JoinInfo joinInfo);

}
