package ru.tecius.telemed.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Перечисление типов разрешений.
 *
 * <p>
 * Определяет возможные типы разрешений для доступа к документам:
 * </p>
 * <ul>
 *   <li>READ - просмотр содержимого</li>
 *   <li>WRITE - редактирование содержимого</li>
 *   <li>DELETE - удаление документа</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum PermissionType {

  READ("просмотр"),
  WRITE("редактирование"),
  DELETE("удаление");

  private final String action;

}
