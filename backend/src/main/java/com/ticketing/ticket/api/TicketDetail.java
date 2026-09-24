package com.ticketing.ticket.api;

import com.ticketing.ticket.domain.Category;
import com.ticketing.ticket.domain.Priority;
import com.ticketing.ticket.domain.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketDetail(
        UUID id,
        String displayId,
        String title,
        TicketStatus status,
        Priority priority,
        Category category,
        String assignee,
        Instant createdAt,
        Instant updatedAt,
        String description,
        String resolution,
        List<CommentDto> comments) {}
