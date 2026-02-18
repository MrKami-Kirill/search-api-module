package ru.tecius.telemed.entity;

/**
 * Перечисление состояний документа.
 *
 * <p>
 * Определяет возможные состояния документа в системе:
 * </p>
 * <ul>
 *   <li>DRAFT - черновик документа</li>
 *   <li>RELEASE - опубликованный документ</li>
 * </ul>
 */
public enum DocumentState {

  DRAFT,
  RELEASE

}
