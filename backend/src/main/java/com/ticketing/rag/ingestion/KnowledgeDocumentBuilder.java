package com.ticketing.rag.ingestion;

import com.ticketing.rag.domain.KnowledgeContentType;
import com.ticketing.ticket.domain.Comment;
import com.ticketing.ticket.domain.Ticket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentBuilder {

    public List<KnowledgeDocumentUnit> buildUnits(Ticket ticket) {
        List<KnowledgeDocumentUnit> units = new ArrayList<>();
        units.add(buildUnit(ticket, KnowledgeContentType.DESCRIPTION, ticket.getId(), ticket.getDescription(), null));
        ticket.getComments().stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .forEach(comment -> units.add(buildUnit(
                        ticket,
                        KnowledgeContentType.COMMENT,
                        comment.getId(),
                        comment.getContent(),
                        comment.getCreatedAt())));
        if (ticket.getResolution() != null && !ticket.getResolution().isBlank()) {
            units.add(buildUnit(
                    ticket, KnowledgeContentType.RESOLUTION, ticket.getId(), ticket.getResolution().trim(), null));
        }
        return units;
    }

    private KnowledgeDocumentUnit buildUnit(
            Ticket ticket,
            KnowledgeContentType contentType,
            UUID sourceRefId,
            String rawContent,
            java.time.Instant commentCreatedAt) {
        String canonicalText = formatCanonicalText(ticket, contentType, rawContent);
        return new KnowledgeDocumentUnit(
                ticket.getId(),
                ticket.getDisplayId(),
                contentType,
                sourceRefId,
                rawContent,
                canonicalText,
                sha256Hex(canonicalText),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCategory(),
                commentCreatedAt);
    }

    static String formatCanonicalText(Ticket ticket, KnowledgeContentType contentType, String rawContent) {
        return "Ticket: " + ticket.getDisplayId() + "\n"
                + "Status: " + ticket.getStatus()
                + " | Priority: " + ticket.getPriority()
                + " | Category: " + ticket.getCategory() + "\n"
                + "Type: " + contentType + "\n"
                + "---\n"
                + rawContent;
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
