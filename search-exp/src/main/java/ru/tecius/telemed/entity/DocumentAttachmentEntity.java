package ru.tecius.telemed.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Сущность вложения документа.
 * Хранит информацию о прикреплённых к документу файлах.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "document_attachments")
public class DocumentAttachmentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "document_id", referencedColumnName = "id")
  private DocumentEntity document;

  private String fileName;

  private String extension;

  private String minioPath;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "creator_id", referencedColumnName = "id")
  private UserEntity creator;

  @CreationTimestamp
  private OffsetDateTime createDate;



}
