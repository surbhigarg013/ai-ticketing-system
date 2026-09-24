package com.ticketing.rag.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IndexStatusResponse(
        UUID ticketId,
        List<IndexDocumentStatus> documents,
        Instant lastIndexedAt,
        int pendingJobs) {}
