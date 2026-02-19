package ru.tecius.telemed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tecius.telemed.dto.request.SearchRequestDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;
import ru.tecius.telemed.entity.MenuItemEntity;
import ru.tecius.telemed.entity.MenuItemEntityCriteriaInfo;
import ru.tecius.telemed.entity.MenuItemEntitySearchInfo;

@Service
public class MenuItemService {

  private final JdbcNativeSqlService<MenuItemEntity> jdbcNativeSqlService;
  private final NativeEntitySqlService<MenuItemEntity> nativeEntitySqlService;
  private final CriteriaEntityService<MenuItemEntity> criteriaEntityService;
  private final ObjectMapper objectMapper;

  public MenuItemService(JdbcTemplate jdbcTemplate,
      EntityManager entityManager,
      MenuItemEntitySearchInfo menuItemEntitySearchInfo,
      MenuItemEntityCriteriaInfo menuItemEntityCriteriaInfo,
      ObjectMapper objectMapper) {
    var menuItemRowMapper = new RowMapper<MenuItemEntity>() {
      @Override
      public @Nullable MenuItemEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        return MenuItemEntity.builder()
            .id(rs.getLong("id"))
            .path(rs.getString("title"))
            .path(rs.getString("path"))
            .isActive(rs.getBoolean("is_active"))
            .inheritParentPermissions(rs.getBoolean("inherit_parent_permissions"))
            .availableEveryone(rs.getBoolean("available_everyone"))
            .createDate(rs.getObject("create_date", OffsetDateTime.class))
            .lastUpdateDate(rs.getObject("last_update_date", OffsetDateTime.class))
            .build();
      }
    };
    this.jdbcNativeSqlService = new JdbcNativeSqlService<>(jdbcTemplate,
        menuItemRowMapper,
        menuItemEntitySearchInfo);
    // Примечание: EntityGraph не поддерживается для нативных SQL запросов в Hibernate
    // Для подгрузки связанных сущностей используйте JPQL или добавьте JOIN'ы в конфигурацию
    this.nativeEntitySqlService = new NativeEntitySqlService<>(
        MenuItemEntity.class,
        entityManager,
        menuItemEntitySearchInfo
    );
    // Criteria API сервис с поддержкой Entity Graph
    this.criteriaEntityService = new CriteriaEntityService<>(
        entityManager,
        menuItemEntityCriteriaInfo
    );
    this.objectMapper = objectMapper;
  }

  @Transactional
  @SneakyThrows
  public SearchResponseDto<MenuItemEntity> search() {
    var request = objectMapper.readValue("""
        {
          "pagination": {
            "page": 0,
            "size": 10
          },
          "sort": [
            {
              "attribute": "createDate",
              "direction": "DESC"
            },
            {
              "attribute": "title",
              "direction": "ASC"
            }
          ],
          "searchData": [
            {
              "attribute": "isActive",
              "value": [
                "true"
              ],
              "operator": "EQUAL"
            },
            {
              "attribute": "attachmentFileName",
              "value": [
                "My_Little"
              ],
              "operator": "NOT_BEGIN"
            },
            {
              "attribute": "attachmentExtension",
              "value": [
                "img"
              ],
              "operator": "NOT_EQUAL"
            },
            {
              "attribute": "createDate",
              "value": [
                "2026-02-12T09:00:00+03:00"
              ],
              "operator": "MORE_OR_EQUAL"
            }
          ]
        }
        """, SearchRequestDto.class);
    // Пример 1: JDBC Native SQL
    var result1 = jdbcNativeSqlService.search(request.searchData(), request.sort(), request.pagination());

    // Пример 2: JPA Native SQL
    var result2 = nativeEntitySqlService.search(request.searchData(), request.sort(), request.pagination());

    // Пример 3: Criteria API
    var result3 = criteriaEntityService.search(request.searchData(), request.sort(), request.pagination());

    return result3;
  }
}
