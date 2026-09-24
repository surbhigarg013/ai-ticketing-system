package com.ticketing.ticket.service;

import com.ticketing.ticket.api.CommentDto;
import com.ticketing.ticket.api.CreateCommentRequest;
import com.ticketing.ticket.api.TicketMapper;
import com.ticketing.ticket.domain.Comment;
import com.ticketing.ticket.domain.Ticket;
import com.ticketing.ticket.event.TicketChangedEvent;
import com.ticketing.ticket.repository.CommentRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final TicketService ticketService;
    private final CommentRepository commentRepository;
    private final TicketMapper ticketMapper;
    private final ApplicationEventPublisher eventPublisher;

    public CommentService(
            TicketService ticketService,
            CommentRepository commentRepository,
            TicketMapper ticketMapper,
            ApplicationEventPublisher eventPublisher) {
        this.ticketService = ticketService;
        this.commentRepository = commentRepository;
        this.ticketMapper = ticketMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CommentDto addComment(UUID ticketId, CreateCommentRequest request) {
        Ticket ticket = ticketService.findTicketWithComments(ticketId);
        Comment comment = new Comment();
        comment.setContent(request.content().trim());
        comment.setAuthor(request.author().trim());
        ticket.addComment(comment);
        commentRepository.save(comment);
        eventPublisher.publishEvent(new TicketChangedEvent(ticketId, TicketChangedEvent.Trigger.COMMENT_ADD));
        return ticketMapper.toCommentDto(comment);
    }
}
