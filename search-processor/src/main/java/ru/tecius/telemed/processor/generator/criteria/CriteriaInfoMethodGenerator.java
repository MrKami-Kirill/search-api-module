package ru.tecius.telemed.processor.generator.criteria;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Optional;
import java.util.Set;
import ru.tecius.telemed.configuration.criteria.CriteriaSearchAttribute;
import ru.tecius.telemed.exception.ValidationException;

public class CriteriaInfoMethodGenerator {

  public void addInterfaceMethods(TypeSpec.Builder classBuilder) {
    addGetEntityClassMethod(classBuilder);
    addGetSimpleAttributesMethod(classBuilder);
    addGetSimpleAttributeByJsonKeyMethod(classBuilder);
    addGetMultipleAttributesMethod(classBuilder);
    addGetMultipleAttributeByJsonKeyMethod(classBuilder);
    addGetAttributeByJsonKeyMethod(classBuilder);
  }

  private void addGetEntityClassMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getEntityClass")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Class.class))
        .addStatement("return ENTITY_CLASS")
        .build());
  }

  private void addGetSimpleAttributesMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getSimpleAttributes")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Set.class, CriteriaSearchAttribute.class))
        .addStatement("return SIMPLE_ATTRIBUTES")
        .build());
  }

  private void addGetSimpleAttributeByJsonKeyMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getSimpleAttributeByJsonKey")
        .addParameter(String.class, "key")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Optional.class, CriteriaSearchAttribute.class))
        .addStatement("""
            return SIMPLE_ATTRIBUTES.stream()
                    .filter(attr -> java.util.Objects.equals(attr.json().key(), key))
                    .findAny()""")
        .build());
  }

  private void addGetMultipleAttributesMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getMultipleAttributes")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Set.class, CriteriaSearchAttribute.class))
        .addStatement("return MULTIPLE_ATTRIBUTES")
        .build());
  }

  private void addGetMultipleAttributeByJsonKeyMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getMultipleAttributeByJsonKey")
        .addParameter(String.class, "key")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(ParameterizedTypeName.get(Optional.class, CriteriaSearchAttribute.class))
        .addStatement("""
            return MULTIPLE_ATTRIBUTES.stream()
                    .filter(attr -> java.util.Objects.equals(attr.json().key(), key))
                    .findAny()""")
        .build());
  }

  private void addGetAttributeByJsonKeyMethod(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(MethodSpec.methodBuilder("getAttributeByJsonKey")
        .addParameter(String.class, "key")
        .addParameter(String.class, "errorMessage")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(CriteriaSearchAttribute.class)
        .addStatement("""
                return getSimpleAttributeByJsonKey(key)
                        .orElseGet(() -> getMultipleAttributeByJsonKey(key)
                            .orElseThrow(() -> new $T(errorMessage)))""",
            ValidationException.class)
        .build());
  }

}
