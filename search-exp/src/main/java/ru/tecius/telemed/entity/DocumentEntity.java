package ru.tecius.telemed.entity;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
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

/**
 * Сущность документа.
 * Хранит основную информацию о документах с поддержкой версионирования.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "documents")
public class DocumentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @OneToOne(fetch = LAZY)
  @JoinColumn(name = "menu_item_id", referencedColumnName = "id")
  private MenuItemEntity menuItem;

  private Boolean allowComments;

  private Boolean showHistory;

  private String htmlContent;

  private Integer version;

  @Enumerated(STRING)
  private DocumentState state;

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

  @OneToOne(mappedBy = "document", cascade = {ALL}, orphanRemoval = true)
  private DocumentTextContentEntity documentTextContent;

  @Fetch(FetchMode.SUBSELECT)
  @OneToMany(mappedBy = "document", orphanRemoval = true)
  @Builder.Default
  private Set<DocumentAttachmentEntity> attachments = new HashSet<>();

  @Fetch(FetchMode.SUBSELECT)
  @OneToMany(mappedBy = "document", orphanRemoval = true)
  @Builder.Default
  private Set<DocumentCommentEntity> comments = new HashSet<>();

}
