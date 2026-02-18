package ru.tecius.telemed.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Сущность текстового содержимого документа.
 * Хранит текстовое представление документа для полнотекстового поиска.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "documents_text_content")
public class DocumentTextContentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @OneToOne(fetch = LAZY)
  @JoinColumn(name = "document_id", referencedColumnName = "id")
  private DocumentEntity document;

  private String content;

  @CreationTimestamp
  private OffsetDateTime createDate;

  @UpdateTimestamp
  private OffsetDateTime lastUpdateDate;



}
