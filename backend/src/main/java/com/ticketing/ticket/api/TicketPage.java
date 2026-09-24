package com.ticketing.ticket.api;

import java.util.List;

public record TicketPage(List<TicketSummary> items, int page, int size, long total) {}
