package com.ticketing.ticket.api;

import java.time.Instant;
import java.util.UUID;

public record CommentDto(UUID id, String content, String author, Instant createdAt) {}
