package ru.tecius.telemed.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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

/**
 * Сущность группы пользователей.
 * Объединяет пользователей для управления правами доступа.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "groups")
public class GroupEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  private String title;

  private Boolean isActive;

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
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "users_groups",
      joinColumns = @JoinColumn(name = "group_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  private Set<UserEntity> users = new HashSet<>();

}
