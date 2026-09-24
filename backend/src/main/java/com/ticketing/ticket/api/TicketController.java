package com.ticketing.ticket.api;

import com.ticketing.ticket.domain.TicketStatus;
import com.ticketing.ticket.service.CommentService;
import com.ticketing.ticket.service.TicketService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;

    public TicketController(TicketService ticketService, CommentService commentService) {
        this.ticketService = ticketService;
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<TicketDetail> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketDetail created = ticketService.createTicket(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public TicketPage listTickets(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), parseSort(sort));
        return ticketService.listTickets(q, status, pageable);
    }

    @GetMapping("/{ticketId}")
    public TicketDetail getTicket(@PathVariable UUID ticketId) {
        return ticketService.getTicket(ticketId);
    }

    @PatchMapping("/{ticketId}")
    public TicketDetail updateTicket(
            @PathVariable UUID ticketId, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.updateTicket(ticketId, request);
    }

    @PatchMapping("/{ticketId}/status")
    public TicketDetail transitionStatus(
            @PathVariable UUID ticketId, @Valid @RequestBody StatusTransitionRequest request) {
        return ticketService.transitionStatus(ticketId, request);
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<CommentDto> addComment(
            @PathVariable UUID ticketId, @Valid @RequestBody CreateCommentRequest request) {
        CommentDto created = commentService.addComment(ticketId, request);
        return ResponseEntity.status(201).body(created);
    }

    private static Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
