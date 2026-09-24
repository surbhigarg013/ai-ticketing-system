package com.ticketing.ticket.api;

import com.ticketing.ticket.domain.Category;
import com.ticketing.ticket.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        @NotBlank String description,
        @NotNull Priority priority,
        @Size(max = 100) String assignee,
        Category category) {}
