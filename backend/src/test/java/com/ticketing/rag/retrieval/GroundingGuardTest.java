package com.ticketing.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketing.shared.config.RagProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

@ExtendWith(MockitoExtension.class)
class GroundingGuardTest {

    private static final double THRESHOLD = 0.5;

    @Mock
    private CosineSimilarityScorer similarityScorer;

    private GroundingGuard groundingGuard;

    @BeforeEach
    void setUp() {
        RagProperties ragProperties = new RagProperties(
                new RagProperties.Retrieval(5, THRESHOLD),
                "No relevant tickets found.",
                new RagProperties.Indexing(3, 2000));
        groundingGuard = new GroundingGuard(ragProperties, similarityScorer);
    }

    @Test
    void filter_returnsEmptyWhenCandidatesEmpty() {
        assertThat(groundingGuard.filter("payment failures", List.of())).isEmpty();
    }

    @Test
    void filter_includesDocumentsAboveSimilarityThreshold() {
        Document included = document("doc-1", Map.of("distance", 0.3));
        Document excluded = document("doc-2", Map.of("distance", 0.7));

        List<Document> result = groundingGuard.filter("payment failures", List.of(included, excluded));

        assertThat(result).containsExactly(included);
    }

    @Test
    void filter_parsesStringDistanceMetadata() {
        Document included = document("doc-1", Map.of("distance", "0.25"));

        List<Document> result = groundingGuard.filter("payment failures", List.of(included));

        assertThat(result).containsExactly(included);
    }

    @Test
    void filter_usesFallbackScorerWhenDistanceMissing() {
        String docId = UUID.randomUUID().toString();
        Document candidate = document(docId, Map.of());

        when(similarityScorer.cosineDistances(eq("payment failures"), eq(List.of(docId))))
                .thenReturn(Map.of(docId, 0.2));

        List<Document> result = groundingGuard.filter("payment failures", List.of(candidate));

        assertThat(result).containsExactly(candidate);
        verify(similarityScorer).cosineDistances("payment failures", List.of(docId));
    }

    @Test
    void filter_usesFallbackScorerWhenDistanceMetadataInvalid() {
        String docId = UUID.randomUUID().toString();
        Document candidate = document(docId, Map.of("distance", "not-a-number"));

        when(similarityScorer.cosineDistances(eq("payment failures"), eq(List.of(docId))))
                .thenReturn(Map.of(docId, 0.1));

        List<Document> result = groundingGuard.filter("payment failures", List.of(candidate));

        assertThat(result).containsExactly(candidate);
    }

    @Test
    void filter_excludesFallbackDocumentsBelowThreshold() {
        String docId = UUID.randomUUID().toString();
        Document candidate = document(docId, Map.of());

        when(similarityScorer.cosineDistances(eq("payment failures"), eq(List.of(docId))))
                .thenReturn(Map.of(docId, 0.8));

        List<Document> result = groundingGuard.filter("payment failures", List.of(candidate));

        assertThat(result).isEmpty();
    }

    @Test
    void filter_excludesFallbackDocumentsWhenDistanceUnavailable() {
        String docId = UUID.randomUUID().toString();
        Document candidate = document(docId, Map.of());

        when(similarityScorer.cosineDistances(eq("payment failures"), eq(List.of(docId))))
                .thenReturn(Map.of());

        List<Document> result = groundingGuard.filter("payment failures", List.of(candidate));

        assertThat(result).isEmpty();
    }

    private static Document document(String id, Map<String, Object> metadata) {
        return new Document(id, "sample context", metadata);
    }
}
