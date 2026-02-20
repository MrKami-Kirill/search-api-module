package ru.tecius.telemed.processor.generator.criteria;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.squareup.javapoet.CodeBlock;
import jakarta.persistence.criteria.JoinType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ru.tecius.telemed.configuration.criteria.CriteriaJoinInfo;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;
import ru.tecius.telemed.configuration.nativ.AttributeType;
import ru.tecius.telemed.configuration.nativ.CriteriaSearchAttributeConfig;

public class CriteriaInfoCodeBlockGenerator {

  public CodeBlock generateCriteriaAttributesBlock(List<CriteriaSearchAttributeConfig> configs) {
    var builder = CodeBlock.builder();

    var allAttributes = new ArrayList<CriteriaSearchAttribute>();
    configs.forEach(config -> allAttributes.addAll(config.attributes()));

    if (allAttributes.isEmpty()) {
      builder.add("$T.of()", Set.class);
      return builder.build();
    }

    builder.add("new $T<$T>($T.asList(\n", LinkedHashSet.class, CriteriaSearchAttribute.class, Arrays.class);

    var allAttributesSize = allAttributes.size();
    for (int i = 0; i < allAttributesSize; i++) {
      var attr = allAttributes.get(i);
      builder.add("\tnew $T(\n", CriteriaSearchAttribute.class);
      builder.add("\t\t$T.$L,\n", AttributeType.class, attr.attributeType());
      builder.add("\t\t$S,\n", attr.jsonField());
      builder.add("\t\t$S,\n", attr.entityPath());
      builder.add("\t\t$T.class,\n", attr.fieldType());

      var joinInfo = attr.joinInfo();
      if (isNotEmpty(joinInfo)) {
        builder.add("\tnew $T<$T>($T.asList(\n", LinkedHashSet.class, CriteriaJoinInfo.class, Arrays.class);
        var joins = new ArrayList<>(attr.joinInfo());
        for (int j = 0; j < joins.size(); j++) {
          var join = joins.get(j);
          builder.add("\tnew $T(\n", CriteriaJoinInfo.class);
          builder.add("\t$S,\n", join.path());
          builder.add("\t$T.$L\n", JoinType.class, join.type());
          builder.add(j < joins.size() - 1 ? "),\n" : ")\n");
        }
        builder.add("\t))\n");
      } else {
        builder.add("\tnull\n");
      }

      builder.add(i < allAttributesSize - 1 ? "),\n" : ")\n");
    }

    builder.add("))");
    return builder.build();
  }

}
