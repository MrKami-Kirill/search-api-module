package ru.tecius.telemed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import ru.tecius.telemed.dto.request.SearchRequestDto;
import ru.tecius.telemed.dto.response.SearchResponseDto;
import ru.tecius.telemed.entity.MenuItemEntity;
import ru.tecius.telemed.entity.MenuItemEntitySearchInfo;

@Service
public class MenuItemService {

  private final NativeSqlService<MenuItemEntity> nativeSqlService;
  private final ObjectMapper objectMapper;

  public MenuItemService(JdbcTemplate jdbcTemplate,
      MenuItemEntitySearchInfo menuItemEntitySearchInfo,
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
    this.nativeSqlService = new NativeSqlService<>(jdbcTemplate,
        menuItemRowMapper,
        menuItemEntitySearchInfo);
    this.objectMapper = objectMapper;
  }

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
              "attribute": "documentId",
              "value": [
                "40",
                "80"
              ],
              "operator": "BETWEEN"
            }
          ]
        }
        """, SearchRequestDto.class);
    return nativeSqlService.search(request.searchData(), request.sort(), request.pagination());
  }
}
