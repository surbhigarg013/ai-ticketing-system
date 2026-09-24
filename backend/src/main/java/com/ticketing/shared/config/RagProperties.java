package com.ticketing.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        Retrieval retrieval,
        String noMatchMessage,
        Indexing indexing) {

    public record Retrieval(int topK, double similarityThreshold) {}

    public record Indexing(int maxRetries, long pollIntervalMs) {}
}
