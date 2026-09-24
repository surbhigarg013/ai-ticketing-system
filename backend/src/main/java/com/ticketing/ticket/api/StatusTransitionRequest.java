package com.ticketing.ticket.api;

import com.ticketing.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(@NotNull TicketStatus status, String resolution) {}
