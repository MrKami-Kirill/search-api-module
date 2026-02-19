package ru.tecius.telemed.processor.util;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

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
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$S", value)
        .build();
  }

}
