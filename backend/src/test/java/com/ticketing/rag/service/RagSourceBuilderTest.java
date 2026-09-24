package com.ticketing.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ticketing.rag.api.Source;
import com.ticketing.shared.config.VectorStoreConfig;
import com.ticketing.ticket.domain.Ticket;
import com.ticketing.ticket.domain.TicketStatus;
import com.ticketing.ticket.repository.TicketRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

@ExtendWith(MockitoExtension.class)
class RagSourceBuilderTest {

    @Mock
    private TicketRepository ticketRepository;

    private RagSourceBuilder ragSourceBuilder;

    @BeforeEach
    void setUp() {
        ragSourceBuilder = new RagSourceBuilder(ticketRepository);
    }

    @Test
    void buildSources_deduplicatesByTicketAndAggregatesContentTypes() {
        UUID ticketUuid = UUID.randomUUID();
        List<Document> grounded = List.of(
                document(ticketUuid, "TKT-1001", "DESCRIPTION"),
                document(ticketUuid, "TKT-1001", "COMMENT"),
                document(ticketUuid, "TKT-1001", "RESOLUTION"));

        List<Source> sources = ragSourceBuilder.buildSources(grounded);

        assertThat(sources).hasSize(1);
        assertThat(sources.getFirst().ticketId()).isEqualTo(ticketUuid.toString());
        assertThat(sources.getFirst().displayId()).isEqualTo("TKT-1001");
        assertThat(sources.getFirst().contentTypes()).containsExactly("description", "comment", "resolution");
    }

    @Test
    void buildSources_resolvesMissingUuidFromDisplayId() {
        UUID ticketUuid = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(ticketUuid);
        ticket.setDisplayId("TKT-1002");
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findByDisplayId("TKT-1002")).thenReturn(Optional.of(ticket));

        List<Source> sources = ragSourceBuilder.buildSources(List.of(document(null, "TKT-1002", "comment")));

        assertThat(sources).hasSize(1);
        assertThat(sources.getFirst().ticketId()).isEqualTo(ticketUuid.toString());
    }

    @Test
    void buildSources_skipsEntriesWithoutResolvableUuid() {
        when(ticketRepository.findByDisplayId("TKT-9999")).thenReturn(Optional.empty());

        List<Source> sources =
                ragSourceBuilder.buildSources(List.of(document(null, "TKT-9999", "description")));

        assertThat(sources).isEmpty();
    }

    @Test
    void buildSources_ignoresUnknownContentTypes() {
        UUID ticketUuid = UUID.randomUUID();
        List<Source> sources = ragSourceBuilder.buildSources(List.of(
                document(ticketUuid, "TKT-1003", "DESCRIPTION"),
                document(ticketUuid, "TKT-1003", "unknown-type")));

        assertThat(sources).hasSize(1);
        assertThat(sources.getFirst().contentTypes()).containsExactly("description");
    }

    private static Document document(UUID ticketUuid, String displayId, String contentType) {
        return new Document(
                "sample text",
                Map.of(
                        VectorStoreConfig.META_TICKET_ID,
                        ticketUuid != null ? ticketUuid.toString() : "",
                        VectorStoreConfig.META_DISPLAY_ID,
                        displayId,
                        VectorStoreConfig.META_CONTENT_TYPE,
                        contentType));
    }
}
