package ru.tecius.telemed.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
 * Сущность комментария к документу.
 * Хранит комментарии с поддержкой древовидной структуры.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "document_comments")
public class DocumentCommentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "parent_id", referencedColumnName = "id")
  private DocumentCommentEntity parent;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "document_id", referencedColumnName = "id")
  private DocumentEntity document;

  private String commentText;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "creator_id", referencedColumnName = "id")
  private UserEntity creator;

  @CreationTimestamp
  private OffsetDateTime createDate;

  @UpdateTimestamp
  private OffsetDateTime lastUpdateDate;

  @Fetch(FetchMode.SUBSELECT)
  @OneToMany(mappedBy = "parent", fetch = LAZY, orphanRemoval = true)
  @Builder.Default
  private Set<DocumentCommentEntity> children = new HashSet<>();



}
