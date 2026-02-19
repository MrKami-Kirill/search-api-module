package ru.tecius.telemed.common;

import java.util.Optional;
import java.util.Set;
import ru.tecius.telemed.configuration.CriteriaJoinInfo;
import ru.tecius.telemed.configuration.CriteriaSearchAttribute;

/**
 * Интерфейс для конфигурации Criteria API поиска.
 * Предоставляет метаданные для построения динамических запросов через JPA Criteria API.
 */
public interface CriteriaInfoInterface<E> {

  /**
   * Возвращает класс сущности.
   */
  Class<E> getEntityClass();

  /**
   * Возвращает все атрибуты для Criteria поиска.
   */
  Set<CriteriaSearchAttribute> getCriteriaAttributes();

  /**
   * Находит атрибут по JSON полю.
   */
  Optional<CriteriaSearchAttribute> getCriteriaAttributeByJsonField(String jsonField);

  /**
   * Возвращает все join'ы, необходимые для поиска.
   */
  Set<CriteriaJoinInfo> getAllJoins();

  /**
   * Проверяет, существует ли атрибут с указанным JSON полем.
   */
  boolean hasAttribute(String jsonField);
}
