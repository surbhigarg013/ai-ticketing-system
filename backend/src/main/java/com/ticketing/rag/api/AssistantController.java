package com.ticketing.rag.api;

import com.ticketing.rag.service.AssistantService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        return ResponseEntity.ok(assistantService.ask(request.question()));
    }

    @GetMapping("/index-status/{ticketId}")
    public ResponseEntity<IndexStatusResponse> getIndexStatus(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(assistantService.getIndexStatus(ticketId));
    }
}
