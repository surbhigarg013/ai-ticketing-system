package com.ticketing.rag.retrieval;

import com.ticketing.shared.config.RagProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
public class GroundingGuard {

    private final RagProperties ragProperties;
    private final CosineSimilarityScorer similarityScorer;

    public GroundingGuard(RagProperties ragProperties, CosineSimilarityScorer similarityScorer) {
        this.ragProperties = ragProperties;
        this.similarityScorer = similarityScorer;
    }

    public List<Document> filter(String question, List<Document> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        double threshold = ragProperties.retrieval().similarityThreshold();
        List<String> documentIds = candidates.stream().map(Document::getId).toList();
        Map<String, Double> distances = similarityScorer.cosineDistances(question, documentIds);

        List<Document> grounded = new ArrayList<>();
        for (Document candidate : candidates) {
            Double distance = distances.get(candidate.getId());
            if (distance != null && (1.0 - distance) >= threshold) {
                grounded.add(candidate);
            }
        }
        return grounded;
    }
}
