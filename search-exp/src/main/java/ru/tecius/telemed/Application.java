package ru.tecius.telemed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс приложения.
 * Точка входа в Spring Boot приложение.
 */
@SpringBootApplication
public class Application {

  /**
   * Главный метод приложения. Запускает Spring Boot приложение с указанными аргументами.
   *
   * @param args аргументы командной строки
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
    // TODO: fix AbstractSqlService instantiation - it's abstract
    // var context = SpringApplication.run(Application.class, args);
    // var entityManager = context.getBean(EntityManager.class);
    // var sqlService = new AbstractSqlService<>(entityManager);
    // sqlService.search(null);
  }

}
