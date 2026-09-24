package com.ticketing.rag.api;

import java.util.List;

public record Source(String ticketId, String displayId, List<String> contentTypes) {}
