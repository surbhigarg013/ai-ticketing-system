package com.ticketing.rag.retrieval;

import com.ticketing.shared.config.RagProperties;
import com.ticketing.shared.exception.AiServiceUnavailableException;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public RetrievalService(VectorStore vectorStore, RagProperties ragProperties) {
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    public List<Document> retrieve(String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(ragProperties.retrieval().topK())
                .similarityThreshold(ragProperties.retrieval().similarityThreshold())
                .build();
        try {
            return vectorStore.similaritySearch(request);
        } catch (Exception ex) {
            throw new AiServiceUnavailableException("Embedding service unavailable", ex);
        }
    }
}
