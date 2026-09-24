package com.ticketing.rag.api;

import java.util.List;

public record AskResponse(String answer, List<Source> sources) {}
