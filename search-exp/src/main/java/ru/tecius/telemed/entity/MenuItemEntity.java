package ru.tecius.telemed.entity;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;
import ru.tecius.telemed.annotation.SearchInfo;

/**
 * Сущность элемента меню. Представляет пункт навигационного меню с поддержкой иерархии.
 */
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "menu_items")
@SearchInfo(schema = "db_knowledge_base",
    table = "menu_items",
    alias = "mi",
    nativeAttributePaths = {
        "menu-item-native-attributes-config.yml"
    },
    criteriaAttributePaths = {
        "menu-item-criteria-attributes-config.yml"
    })
public class MenuItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "parent_id", referencedColumnName = "id")
  private MenuItemEntity parent;

  @OneToOne(mappedBy = "menuItem", cascade = ALL, orphanRemoval = true)
  private DocumentEntity document;

  private String title;

  private String path;

  private Boolean isActive;

  private Boolean inheritParentPermissions;

  private Boolean availableEveryone;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "creator_id", referencedColumnName = "id")
  private UserEntity creator;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "last_modifier_id", referencedColumnName = "id")
  private UserEntity lastModifier;

  @CreationTimestamp
  private OffsetDateTime createDate;

  @UpdateTimestamp
  private OffsetDateTime lastUpdateDate;

  @Fetch(FetchMode.SUBSELECT)
  @OneToMany(mappedBy = "parent", orphanRemoval = true)
  @Builder.Default
  private Set<MenuItemEntity> children = new HashSet<>();

  @Fetch(FetchMode.SUBSELECT)
  @OneToMany(mappedBy = "menuItem", fetch = LAZY, orphanRemoval = true)
  @Builder.Default
  private Set<PermissionEntity> permissions = new HashSet<>();

}
