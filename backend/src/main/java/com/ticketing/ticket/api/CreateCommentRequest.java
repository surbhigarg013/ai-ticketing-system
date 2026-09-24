package com.ticketing.ticket.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank String content, @NotBlank @Size(max = 100) String author) {}
