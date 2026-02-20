package ru.tecius.telemed.processor.generator.criteria;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Optional;
import java.util.Set;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;

public class CriteriaInfoMethodGenerator {

  public void addInterfaceMethods(TypeSpec.Builder classBuilder) {
    addGetEntityClassMethod(classBuilder);
    addGetCriteriaAttributesMethod(classBuilder);
    addGetCriteriaAttributeByJsonFieldMethod(classBuilder);
  }

  private void addGetEntityClassMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getEntityClass")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Class.class))
        .addStatement("return ENTITY_CLASS")
        .build());
  }

  private void addGetCriteriaAttributesMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getCriteriaAttributes")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Set.class, CriteriaSearchAttribute.class))
        .addStatement("return CRITERIA_ATTRIBUTES")
        .build());
  }

  private void addGetCriteriaAttributeByJsonFieldMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getCriteriaAttributeByJsonField")
        .addParameter(String.class, "jsonField")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Optional.class, CriteriaSearchAttribute.class))
        .addStatement("""
            return CRITERIA_ATTRIBUTES.stream()
                    .filter(attr -> java.util.Objects.equals(attr.jsonField(), jsonField))
                    .findAny()""")
        .build());
  }

}
