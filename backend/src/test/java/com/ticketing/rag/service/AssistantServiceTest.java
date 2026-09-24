package com.ticketing.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketing.rag.api.AskResponse;
import com.ticketing.rag.api.IndexStatusResponse;
import com.ticketing.rag.domain.IndexingJobStatus;
import com.ticketing.rag.domain.KnowledgeContentType;
import com.ticketing.rag.domain.KnowledgeDocument;
import com.ticketing.rag.repository.IndexingJobRepository;
import com.ticketing.rag.repository.KnowledgeDocumentRepository;
import com.ticketing.rag.retrieval.GroundingGuard;
import com.ticketing.rag.retrieval.RetrievalService;
import com.ticketing.shared.config.RagProperties;
import com.ticketing.shared.exception.ResourceNotFoundException;
import com.ticketing.ticket.repository.TicketRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private GroundingGuard groundingGuard;

    @Mock
    private RagSourceBuilder ragSourceBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private IndexingJobRepository indexingJobRepository;

    @Mock
    private TicketRepository ticketRepository;

    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        RagProperties ragProperties = new RagProperties(
                new RagProperties.Retrieval(5, 0.5),
                "No relevant tickets found.",
                new RagProperties.Indexing(3, 2000));
        assistantService = new AssistantService(
                retrievalService,
                groundingGuard,
                ragSourceBuilder,
                chatClient,
                ragProperties,
                knowledgeDocumentRepository,
                indexingJobRepository,
                ticketRepository);
    }

    @Test
    void ask_returnsNoMatchWhenGroundingIsEmpty() {
        Document retrieved = document();
        when(retrievalService.retrieve("payment failures")).thenReturn(List.of(retrieved));
        when(groundingGuard.filter(eq("payment failures"), anyList())).thenReturn(List.of());

        AskResponse response = assistantService.ask("payment failures");

        assertThat(response.answer()).isEqualTo("No relevant tickets found.");
        assertThat(response.sources()).isEmpty();
        verify(ragSourceBuilder, never()).buildSources(anyList());
    }

    @Test
    void ask_returnsNoMatchWhenSourcesCannotBeBuilt() {
        Document groundedDoc = document();
        when(retrievalService.retrieve("payment failures")).thenReturn(List.of(groundedDoc));
        when(groundingGuard.filter(eq("payment failures"), anyList())).thenReturn(List.of(groundedDoc));
        when(ragSourceBuilder.buildSources(anyList())).thenReturn(List.of());

        AskResponse response = assistantService.ask("payment failures");

        assertThat(response.answer()).isEqualTo("No relevant tickets found.");
        assertThat(response.sources()).isEmpty();
        verify(chatClient, never()).prompt();
    }

    @Test
    void ask_doesNotCallLlmWhenSourcesEmptyEvenIfGrounded() {
        Document groundedDoc = document();
        when(retrievalService.retrieve("anything")).thenReturn(List.of(groundedDoc));
        when(groundingGuard.filter(eq("anything"), anyList())).thenReturn(List.of(groundedDoc));
        when(ragSourceBuilder.buildSources(anyList())).thenReturn(List.of());

        assistantService.ask("anything");

        verify(chatClient, never()).prompt();
    }

    @Test
    void getIndexStatus_throwsWhenTicketMissing() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.existsById(ticketId)).thenReturn(false);

        assertThatThrownBy(() -> assistantService.getIndexStatus(ticketId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(ticketId.toString());
    }

    @Test
    void getIndexStatus_returnsDocumentStatusesAndPendingJobs() {
        UUID ticketId = UUID.randomUUID();
        Instant indexedAt = Instant.parse("2025-09-01T10:00:00Z");

        KnowledgeDocument document = new KnowledgeDocument();
        document.setContentType(KnowledgeContentType.DESCRIPTION);
        document.setIndexedAt(indexedAt);
        document.setTextHash("abc123");

        when(ticketRepository.existsById(ticketId)).thenReturn(true);
        when(knowledgeDocumentRepository.findByTicketId(ticketId)).thenReturn(List.of(document));
        when(indexingJobRepository.countByTicketIdAndStatusIn(
                        ticketId, List.of(IndexingJobStatus.PENDING, IndexingJobStatus.PROCESSING)))
                .thenReturn(2L);

        IndexStatusResponse response = assistantService.getIndexStatus(ticketId);

        assertThat(response.ticketId()).isEqualTo(ticketId);
        assertThat(response.documents()).hasSize(1);
        assertThat(response.documents().getFirst().contentType()).isEqualTo("description");
        assertThat(response.documents().getFirst().textHash()).isEqualTo("abc123");
        assertThat(response.documents().getFirst().indexedAt()).isEqualTo(indexedAt);
        assertThat(response.lastIndexedAt()).isEqualTo(indexedAt);
        assertThat(response.pendingJobs()).isEqualTo(2);
    }

    private static Document document() {
        return new Document("payment failure context", Map.of("displayId", "TKT-1001"));
    }
}
