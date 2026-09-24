package com.ticketing.rag.api;

import java.time.Instant;

public record IndexDocumentStatus(String contentType, Instant indexedAt, String textHash) {}
