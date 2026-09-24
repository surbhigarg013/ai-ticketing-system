package com.ticketing.rag.ingestion;

import com.ticketing.rag.domain.KnowledgeDocument;
import com.ticketing.rag.domain.IndexingJob;
import com.ticketing.rag.domain.IndexingJobStatus;
import com.ticketing.rag.repository.IndexingJobRepository;
import com.ticketing.rag.repository.KnowledgeDocumentRepository;
import com.ticketing.shared.config.RagProperties;
import com.ticketing.shared.config.VectorStoreConfig;
import com.ticketing.shared.exception.ResourceNotFoundException;
import com.ticketing.ticket.domain.Ticket;
import com.ticketing.ticket.repository.TicketRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IndexingJobProcessor {

    private final IndexingJobRepository indexingJobRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final TicketRepository ticketRepository;
    private final KnowledgeDocumentBuilder knowledgeDocumentBuilder;
    private final VectorStore vectorStore;
    private final VectorStoreMetadataUpdater metadataUpdater;
    private final RagProperties ragProperties;
    private final TransactionTemplate transactionTemplate;
    private final String embeddingModelName;

    public IndexingJobProcessor(
            IndexingJobRepository indexingJobRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            TicketRepository ticketRepository,
            KnowledgeDocumentBuilder knowledgeDocumentBuilder,
            VectorStore vectorStore,
            VectorStoreMetadataUpdater metadataUpdater,
            RagProperties ragProperties,
            TransactionTemplate transactionTemplate,
            @Value("${spring.ai.ollama.embedding.options.model:}") String ollamaEmbeddingModel,
            @Value("${spring.ai.openai.embedding.options.model:}") String openAiEmbeddingModel) {
        this.indexingJobRepository = indexingJobRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.ticketRepository = ticketRepository;
        this.knowledgeDocumentBuilder = knowledgeDocumentBuilder;
        this.vectorStore = vectorStore;
        this.metadataUpdater = metadataUpdater;
        this.ragProperties = ragProperties;
        this.transactionTemplate = transactionTemplate;
        this.embeddingModelName = !ollamaEmbeddingModel.isBlank() ? ollamaEmbeddingModel : openAiEmbeddingModel;
    }

    @Scheduled(fixedDelayString = "${app.rag.indexing.poll-interval-ms}")
    public void pollPendingJobs() {
        List<IndexingJob> pendingJobs =
                indexingJobRepository.findByStatus(IndexingJobStatus.PENDING, PageRequest.of(0, 10));
        for (IndexingJob job : pendingJobs) {
            transactionTemplate.executeWithoutResult(status -> processJob(job.getId()));
        }
    }

    void processJob(UUID jobId) {
        IndexingJob job = indexingJobRepository
                .findById(jobId)
                .orElse(null);
        if (job == null || job.getStatus() != IndexingJobStatus.PENDING) {
            return;
        }

        job.setStatus(IndexingJobStatus.PROCESSING);
        indexingJobRepository.save(job);

        try {
            indexTicket(job.getTicketId());
            job.setStatus(IndexingJobStatus.COMPLETED);
            job.setLastError(null);
        } catch (Exception ex) {
            job.setAttempts(job.getAttempts() + 1);
            job.setLastError(truncateError(ex));
            if (job.getAttempts() >= ragProperties.indexing().maxRetries()) {
                job.setStatus(IndexingJobStatus.FAILED);
            } else {
                job.setStatus(IndexingJobStatus.PENDING);
            }
        }
        job.setProcessedAt(Instant.now());
        indexingJobRepository.save(job);
    }

    private void indexTicket(UUID ticketId) {
        Ticket ticket = ticketRepository
                .findByIdWithComments(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        List<KnowledgeDocumentUnit> desiredUnits = knowledgeDocumentBuilder.buildUnits(ticket);
        Set<String> desiredKeys = new HashSet<>();
        for (KnowledgeDocumentUnit unit : desiredUnits) {
            desiredKeys.add(unitKey(unit.contentType(), unit.sourceRefId()));
        }

        List<KnowledgeDocument> existingDocuments = knowledgeDocumentRepository.findByTicketId(ticketId);
        for (KnowledgeDocument existing : existingDocuments) {
            if (!desiredKeys.contains(unitKey(existing.getContentType(), existing.getSourceRefId()))) {
                removeDocument(existing);
            }
        }

        for (KnowledgeDocumentUnit unit : desiredUnits) {
            knowledgeDocumentRepository
                    .findByTicketIdAndContentTypeAndSourceRefId(
                            unit.ticketId(), unit.contentType(), unit.sourceRefId())
                    .ifPresentOrElse(
                            existing -> syncExistingDocument(existing, unit),
                            () -> createAndEmbed(unit));
        }
    }

    private void syncExistingDocument(KnowledgeDocument existing, KnowledgeDocumentUnit unit) {
        if (!existing.getTextHash().equals(unit.textHash())) {
            reEmbed(existing, unit);
            return;
        }
        if (metadataChanged(existing, unit)) {
            applyMetadata(existing, unit);
            knowledgeDocumentRepository.save(existing);
            metadataUpdater.updateMetadata(existing.getId(), buildMetadata(existing, unit.displayId()));
        }
    }

    private void createAndEmbed(KnowledgeDocumentUnit unit) {
        KnowledgeDocument document = new KnowledgeDocument();
        applyUnitFields(document, unit);
        knowledgeDocumentRepository.save(document);
        vectorStore.add(List.of(toVectorDocument(document, unit.displayId())));
        document.setEmbeddingModel(embeddingModelName);
        document.setIndexedAt(Instant.now());
        knowledgeDocumentRepository.save(document);
    }

    private void reEmbed(KnowledgeDocument existing, KnowledgeDocumentUnit unit) {
        vectorStore.delete(List.of(existing.getId().toString()));
        applyUnitFields(existing, unit);
        vectorStore.add(List.of(toVectorDocument(existing, unit.displayId())));
        existing.setEmbeddingModel(embeddingModelName);
        existing.setIndexedAt(Instant.now());
        knowledgeDocumentRepository.save(existing);
    }

    private void removeDocument(KnowledgeDocument document) {
        vectorStore.delete(List.of(document.getId().toString()));
        knowledgeDocumentRepository.delete(document);
    }

    private void applyUnitFields(KnowledgeDocument document, KnowledgeDocumentUnit unit) {
        document.setTicketId(unit.ticketId());
        document.setContentType(unit.contentType());
        document.setSourceRefId(unit.sourceRefId());
        document.setCanonicalText(unit.canonicalText());
        document.setTextHash(unit.textHash());
        applyMetadata(document, unit);
    }

    private void applyMetadata(KnowledgeDocument document, KnowledgeDocumentUnit unit) {
        document.setTicketStatus(unit.ticketStatus());
        document.setTicketPriority(unit.ticketPriority());
        document.setTicketAssignee(unit.ticketAssignee());
        document.setTicketCategory(unit.ticketCategory());
        document.setCommentCreatedAt(unit.commentCreatedAt());
    }

    private boolean metadataChanged(KnowledgeDocument existing, KnowledgeDocumentUnit unit) {
        return existing.getTicketStatus() != unit.ticketStatus()
                || existing.getTicketPriority() != unit.ticketPriority()
                || !Objects.equals(existing.getTicketAssignee(), unit.ticketAssignee())
                || existing.getTicketCategory() != unit.ticketCategory();
    }

    private Document toVectorDocument(KnowledgeDocument document, String displayId) {
        return new Document(
                document.getId().toString(), document.getCanonicalText(), buildMetadata(document, displayId));
    }

    private Map<String, Object> buildMetadata(KnowledgeDocument document, String displayId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(VectorStoreConfig.META_KNOWLEDGE_DOCUMENT_ID, document.getId().toString());
        metadata.put(VectorStoreConfig.META_TICKET_ID, document.getTicketId().toString());
        metadata.put(VectorStoreConfig.META_DISPLAY_ID, displayId);
        metadata.put(VectorStoreConfig.META_CONTENT_TYPE, document.getContentType().name());
        metadata.put(VectorStoreConfig.META_STATUS, document.getTicketStatus().name());
        metadata.put(VectorStoreConfig.META_PRIORITY, document.getTicketPriority().name());
        metadata.put(VectorStoreConfig.META_CATEGORY, document.getTicketCategory().name());
        return metadata;
    }

    private static String unitKey(
            com.ticketing.rag.domain.KnowledgeContentType contentType, UUID sourceRefId) {
        return contentType.name() + ":" + sourceRefId;
    }

    private static String truncateError(Exception ex) {
        String message = ex.getMessage();
        if (message == null) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
