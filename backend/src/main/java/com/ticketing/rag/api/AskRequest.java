package com.ticketing.rag.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank @Size(min = 3, max = 2000) String question) {}
