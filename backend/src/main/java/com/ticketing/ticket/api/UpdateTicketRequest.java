package com.ticketing.ticket.api;

import com.ticketing.ticket.domain.Category;
import com.ticketing.ticket.domain.Priority;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(min = 1, max = 200) String title,
        @Size(min = 1) String description,
        Priority priority,
        @Size(max = 100) String assignee,
        Category category) {}
