package com.ticketing.rag.ingestion;

import com.ticketing.rag.domain.KnowledgeContentType;
import com.ticketing.ticket.domain.Category;
import com.ticketing.ticket.domain.Priority;
import com.ticketing.ticket.domain.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocumentUnit(
        UUID ticketId,
        String displayId,
        KnowledgeContentType contentType,
        UUID sourceRefId,
        String rawContent,
        String canonicalText,
        String textHash,
        TicketStatus ticketStatus,
        Priority ticketPriority,
        String ticketAssignee,
        Category ticketCategory,
        Instant commentCreatedAt) {}
