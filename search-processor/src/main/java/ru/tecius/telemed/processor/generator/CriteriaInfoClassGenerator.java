package ru.tecius.telemed.processor.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.RequiredArgsConstructor;
import ru.tecius.telemed.common.CriteriaInfoInterface;
import ru.tecius.telemed.configuration.CriteriaJoinInfo;
import ru.tecius.telemed.configuration.CriteriaSearchAttribute;
import ru.tecius.telemed.configuration.CriteriaSearchAttributeConfig;

/**
 * Генератор классов для CriteriaInfoInterface.
 * Генерирует реализацию интерфейса на основе аннотации @SearchInfo и YML конфигурации.
 */
@RequiredArgsConstructor
public class CriteriaInfoClassGenerator {

  private final CriteriaInfoMethodGenerator methodGenerator = new CriteriaInfoMethodGenerator();
  private final CriteriaInfoCodeBlockGenerator codeBlockGenerator = new CriteriaInfoCodeBlockGenerator();

  /**
   * Генерирует класс, реализующий CriteriaInfoInterface.
   *
   * @param typeElement      сущность
   * @param criteriaConfigs  конфигурация Criteria атрибутов
   * @return сгенерированный класс
   */
  public TypeSpec generateClassSpec(
      TypeElement typeElement,
      List<CriteriaSearchAttributeConfig> criteriaConfigs
  ) {
    var className = getClassName(typeElement);

    var entityClassName = ClassName.get(typeElement);
    var criteriaInfoInterface = ParameterizedTypeName.get(
        ClassName.get(CriteriaInfoInterface.class),
        entityClassName);

    var classBuilder = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(org.springframework.stereotype.Component.class)
        .addSuperinterface(criteriaInfoInterface);

    addStaticConstants(classBuilder, entityClassName, criteriaConfigs);
    methodGenerator.addInterfaceMethods(classBuilder, entityClassName);

    return classBuilder.build();
  }

  private String getClassName(TypeElement typeElement) {
    return typeElement.getSimpleName() + "CriteriaInfo";
  }

  private void addStaticConstants(
      TypeSpec.Builder classBuilder,
      ClassName entityClass,
      List<CriteriaSearchAttributeConfig> criteriaConfigs
  ) {
    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(Class.class),
            "ENTITY_CLASS",
            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer("$T.class", entityClass)
        .build());

    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(Set.class, CriteriaSearchAttribute.class),
            "CRITERIA_ATTRIBUTES",
            Modifier.PRIVATE, Modifier.FINAL)
        .initializer(codeBlockGenerator.generateCriteriaAttributesBlock(criteriaConfigs))
        .build());

    classBuilder.addField(FieldSpec.builder(
            ParameterizedTypeName.get(Set.class, CriteriaJoinInfo.class),
            "ALL_JOINS",
            Modifier.PRIVATE, Modifier.FINAL)
        .initializer(codeBlockGenerator.generateAllJoinsBlock(criteriaConfigs))
        .build());
  }
}
