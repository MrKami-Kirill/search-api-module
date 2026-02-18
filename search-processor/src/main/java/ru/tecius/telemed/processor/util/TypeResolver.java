package ru.tecius.telemed.processor.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.TypeElement;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TypeResolver {

  public static TypeName getInterfaceTypeName(TypeElement typeElement) {
    return ParameterizedTypeName.get(
        ClassName.get("ru.tecius.telemed.common", "SearchInfoInterface"),
        TypeName.get(typeElement.asType())
    );
  }

  public static FieldSpec createStaticStringField(String name, String value) {
    return FieldSpec.builder(String.class, name)
        .addModifiers(javax.lang.model.element.Modifier.PRIVATE,
            javax.lang.model.element.Modifier.STATIC,
            javax.lang.model.element.Modifier.FINAL)
        .initializer("$S", value)
        .build();
  }

}
