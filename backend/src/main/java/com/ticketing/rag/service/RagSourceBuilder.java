package com.ticketing.rag.service;

import com.ticketing.rag.api.Source;
import com.ticketing.rag.domain.KnowledgeContentType;
import com.ticketing.shared.config.VectorStoreConfig;
import com.ticketing.ticket.repository.TicketRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class RagSourceBuilder {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("description", "comment", "resolution");

    private final TicketRepository ticketRepository;

    public RagSourceBuilder(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<Source> buildSources(List<Document> grounded) {
        Map<String, TicketSourceAccumulator> byTicket = new LinkedHashMap<>();
        for (Document document : grounded) {
            Map<String, Object> metadata = document.getMetadata();
            String ticketId = stringMetadata(metadata, VectorStoreConfig.META_TICKET_ID);
            String displayId = stringMetadata(metadata, VectorStoreConfig.META_DISPLAY_ID);
            String contentType = normalizeContentType(stringMetadata(metadata, VectorStoreConfig.META_CONTENT_TYPE));
            if (displayId.isBlank() || contentType == null) {
                continue;
            }

            String resolvedTicketId = ticketId.isBlank()
                    ? ticketRepository
                            .findByDisplayId(displayId)
                            .map(ticket -> ticket.getId().toString())
                            .orElse("")
                    : ticketId;
            if (resolvedTicketId.isBlank()) {
                continue;
            }

            byTicket
                    .computeIfAbsent(
                            resolvedTicketId,
                            ignored -> new TicketSourceAccumulator(resolvedTicketId, displayId))
                    .contentTypes()
                    .add(contentType);
        }

        return byTicket.values().stream()
                .map(accumulator -> new Source(
                        accumulator.ticketId(),
                        accumulator.displayId(),
                        List.copyOf(accumulator.contentTypes())))
                .toList();
    }

    private static String normalizeContentType(String raw) {
        if (raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (ALLOWED_CONTENT_TYPES.contains(normalized)) {
            return normalized;
        }
        try {
            return KnowledgeContentType.valueOf(raw.toUpperCase(Locale.ROOT))
                    .name()
                    .toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String stringMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : "";
    }

    private record TicketSourceAccumulator(String ticketId, String displayId, LinkedHashSet<String> contentTypes) {
        TicketSourceAccumulator(String ticketId, String displayId) {
            this(ticketId, displayId, new LinkedHashSet<>());
        }
    }
}
