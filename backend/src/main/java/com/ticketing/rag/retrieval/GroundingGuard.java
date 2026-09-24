package com.ticketing.rag.retrieval;

import com.ticketing.shared.config.RagProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

@Service
public class GroundingGuard {

    private static final Logger log = LoggerFactory.getLogger(GroundingGuard.class);
    private static final String DISTANCE_METADATA_KEY = "distance";

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
        List<Document> grounded = new ArrayList<>();
        List<Document> missingDistance = new ArrayList<>();

        for (Document candidate : candidates) {
            Double distance = distanceFromMetadata(candidate.getMetadata());
            if (distance == null) {
                missingDistance.add(candidate);
                continue;
            }
            if (similarityFromDistance(distance) >= threshold) {
                grounded.add(candidate);
            }
        }

        if (!missingDistance.isEmpty()) {
            log.debug(
                    "Computing fallback distances for {} retrieved documents missing vector metadata",
                    missingDistance.size());
            Map<String, Double> fallbackDistances = similarityScorer.cosineDistances(
                    question, missingDistance.stream().map(Document::getId).toList());
            for (Document candidate : missingDistance) {
                Double distance = fallbackDistances.get(candidate.getId());
                if (distance == null) {
                    log.warn(
                            "Excluding retrieved document {} — distance unavailable for question preview: {}",
                            candidate.getId(),
                            truncate(question));
                    continue;
                }
                if (similarityFromDistance(distance) >= threshold) {
                    grounded.add(candidate);
                }
            }
        }

        return grounded;
    }

    private static double similarityFromDistance(double distance) {
        return 1.0 - distance;
    }

    private static Double distanceFromMetadata(Map<String, Object> metadata) {
        Object value = metadata.get(DISTANCE_METADATA_KEY);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 80 ? value : value.substring(0, 80) + "...";
    }
}
