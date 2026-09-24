package com.ticketing.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketing.shared.config.RagProperties;
import com.ticketing.shared.exception.AiServiceUnavailableException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private VectorStore vectorStore;

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        RagProperties ragProperties = new RagProperties(
                new RagProperties.Retrieval(5, 0.5),
                "No relevant tickets found.",
                new RagProperties.Indexing(3, 2000));
        retrievalService = new RetrievalService(vectorStore, ragProperties);
    }

    @Test
    void retrieve_usesConfiguredTopKAndThreshold() {
        Document document = new Document("context", Map.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));

        List<Document> result = retrievalService.retrieve("payment failures");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertThat(request.getQuery()).isEqualTo("payment failures");
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.5);
        assertThat(result).containsExactly(document);
    }

    @Test
    void retrieve_wrapsEmbeddingFailures() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("vector store down"));

        assertThatThrownBy(() -> retrievalService.retrieve("payment failures"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessage("Embedding service unavailable");
    }
}
