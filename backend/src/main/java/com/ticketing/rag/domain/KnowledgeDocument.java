package com.ticketing.rag.domain;

import com.ticketing.ticket.domain.Category;
import com.ticketing.ticket.domain.Priority;
import com.ticketing.ticket.domain.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_document")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeDocument {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "content_type", nullable = false, columnDefinition = "knowledge_content_type")
    private KnowledgeContentType contentType;

    @Column(name = "source_ref_id")
    private UUID sourceRefId;

    @Column(name = "canonical_text", nullable = false, columnDefinition = "TEXT")
    private String canonicalText;

    @Column(name = "text_hash", nullable = false, length = 64)
    private String textHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ticket_status", nullable = false, columnDefinition = "ticket_status")
    private TicketStatus ticketStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ticket_priority", nullable = false, columnDefinition = "ticket_priority")
    private Priority ticketPriority;

    @Column(name = "ticket_assignee", length = 100)
    private String ticketAssignee;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ticket_category", nullable = false, columnDefinition = "ticket_category")
    private Category ticketCategory;

    @Column(name = "comment_created_at")
    private Instant commentCreatedAt;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
