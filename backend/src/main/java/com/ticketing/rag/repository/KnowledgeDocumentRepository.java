package com.ticketing.rag.repository;

import com.ticketing.rag.domain.KnowledgeContentType;
import com.ticketing.rag.domain.KnowledgeDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    List<KnowledgeDocument> findByTicketId(UUID ticketId);

    Optional<KnowledgeDocument> findByTicketIdAndContentTypeAndSourceRefId(
            UUID ticketId, KnowledgeContentType contentType, UUID sourceRefId);
}
