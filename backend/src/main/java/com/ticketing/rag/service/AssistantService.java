package com.ticketing.rag.service;

import com.ticketing.rag.api.AskResponse;
import com.ticketing.rag.api.IndexDocumentStatus;
import com.ticketing.rag.api.IndexStatusResponse;
import com.ticketing.rag.api.Source;
import com.ticketing.rag.domain.IndexingJobStatus;
import com.ticketing.rag.domain.KnowledgeDocument;
import com.ticketing.rag.repository.IndexingJobRepository;
import com.ticketing.rag.repository.KnowledgeDocumentRepository;
import com.ticketing.rag.retrieval.GroundingGuard;
import com.ticketing.rag.retrieval.RetrievalService;
import com.ticketing.shared.config.RagProperties;
import com.ticketing.shared.config.VectorStoreConfig;
import com.ticketing.shared.exception.AiServiceUnavailableException;
import com.ticketing.shared.exception.ResourceNotFoundException;
import com.ticketing.ticket.repository.TicketRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantService {

    private static final String SYSTEM_PROMPT =
            """
            You are a support knowledge assistant. Answer the user's question using ONLY the ticket \
            context provided below. Do not use outside knowledge. If the context does not contain \
            enough information, say you cannot answer from the ticket history. Reference ticket IDs \
            from the context when relevant.""";

    private final RetrievalService retrievalService;
    private final GroundingGuard groundingGuard;
    private final ChatClient chatClient;
    private final RagProperties ragProperties;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final IndexingJobRepository indexingJobRepository;
    private final TicketRepository ticketRepository;

    public AssistantService(
            RetrievalService retrievalService,
            GroundingGuard groundingGuard,
            ChatClient chatClient,
            RagProperties ragProperties,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            IndexingJobRepository indexingJobRepository,
            TicketRepository ticketRepository) {
        this.retrievalService = retrievalService;
        this.groundingGuard = groundingGuard;
        this.chatClient = chatClient;
        this.ragProperties = ragProperties;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.indexingJobRepository = indexingJobRepository;
        this.ticketRepository = ticketRepository;
    }

    public AskResponse ask(String question) {
        List<Document> retrieved = retrievalService.retrieve(question);
        List<Document> grounded = groundingGuard.filter(question, retrieved);

        if (grounded.isEmpty()) {
            return new AskResponse(ragProperties.noMatchMessage(), List.of());
        }

        String answer = generateAnswer(question, grounded);
        return new AskResponse(answer, buildSources(grounded));
    }

    @Transactional(readOnly = true)
    public IndexStatusResponse getIndexStatus(UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found: " + ticketId);
        }

        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByTicketId(ticketId);
        List<IndexDocumentStatus> documentStatuses = documents.stream()
                .map(doc -> new IndexDocumentStatus(
                        doc.getContentType().name().toLowerCase(), doc.getIndexedAt(), doc.getTextHash()))
                .toList();

        Instant lastIndexedAt = documents.stream()
                .map(KnowledgeDocument::getIndexedAt)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        int pendingJobs = (int) indexingJobRepository.countByTicketIdAndStatusIn(
                ticketId, List.of(IndexingJobStatus.PENDING, IndexingJobStatus.PROCESSING));

        return new IndexStatusResponse(ticketId, documentStatuses, lastIndexedAt, pendingJobs);
    }

    private String generateAnswer(String question, List<Document> grounded) {
        String context = grounded.stream()
                .map(Document::getText)
                .reduce((left, right) -> left + "\n---\n" + right)
                .orElse("");

        try {
            return chatClient
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user("Ticket context:\n" + context + "\n\nQuestion: " + question)
                    .call()
                    .content();
        } catch (Exception ex) {
            throw new AiServiceUnavailableException("LLM service unavailable", ex);
        }
    }

    private List<Source> buildSources(List<Document> grounded) {
        Map<String, TicketSourceAccumulator> byTicket = new LinkedHashMap<>();
        for (Document document : grounded) {
            Map<String, Object> metadata = document.getMetadata();
            String ticketId = stringMetadata(metadata, VectorStoreConfig.META_TICKET_ID);
            String displayId = stringMetadata(metadata, VectorStoreConfig.META_DISPLAY_ID);
            String contentType = stringMetadata(metadata, VectorStoreConfig.META_CONTENT_TYPE);
            if (displayId.isBlank() || contentType.isBlank()) {
                continue;
            }

            String key = !ticketId.isBlank() ? ticketId : displayId;
            byTicket
                    .computeIfAbsent(key, ignored -> new TicketSourceAccumulator(ticketId, displayId))
                    .contentTypes()
                    .add(contentType.toLowerCase());
        }

        return byTicket.values().stream()
                .map(accumulator -> new Source(
                        accumulator.ticketId(),
                        accumulator.displayId(),
                        List.copyOf(accumulator.contentTypes())))
                .toList();
    }

    private record TicketSourceAccumulator(String ticketId, String displayId, LinkedHashSet<String> contentTypes) {
        TicketSourceAccumulator(String ticketId, String displayId) {
            this(ticketId, displayId, new LinkedHashSet<>());
        }
    }

    private static String stringMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : "";
    }
}
