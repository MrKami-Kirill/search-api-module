package ru.tecius.telemed.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(CLASS)
public @interface SearchInfo {

  String schema();

  String table();

  String alias() default "";

  String[] nativeAttributePaths() default {};

  String[] criteriaAttributePaths() default {};

}
