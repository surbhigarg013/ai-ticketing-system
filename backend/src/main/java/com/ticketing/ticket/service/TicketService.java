package com.ticketing.ticket.service;

import com.ticketing.shared.exception.ResourceNotFoundException;
import com.ticketing.shared.exception.ValidationException;
import com.ticketing.ticket.api.CreateTicketRequest;
import com.ticketing.ticket.api.StatusTransitionRequest;
import com.ticketing.ticket.api.TicketDetail;
import com.ticketing.ticket.api.TicketMapper;
import com.ticketing.ticket.api.TicketPage;
import com.ticketing.ticket.api.UpdateTicketRequest;
import com.ticketing.ticket.domain.Category;
import com.ticketing.ticket.domain.Ticket;
import com.ticketing.ticket.domain.TicketStateMachine;
import com.ticketing.ticket.domain.TicketStatus;
import com.ticketing.ticket.event.TicketChangedEvent;
import com.ticketing.ticket.repository.TicketRepository;
import com.ticketing.ticket.repository.TicketSpecifications;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final ApplicationEventPublisher eventPublisher;

    public TicketService(
            TicketRepository ticketRepository,
            TicketMapper ticketMapper,
            ApplicationEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TicketDetail createTicket(CreateTicketRequest request) {
        String title = normalizeRequiredText(request.title(), "title");
        Ticket ticket = new Ticket();
        ticket.setDisplayId(generateDisplayId());
        ticket.setTitle(title);
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        ticket.setAssignee(trimToNull(request.assignee()));
        ticket.setCategory(request.category() != null ? request.category() : Category.GENERAL);
        Ticket saved = ticketRepository.save(ticket);
        publishEvent(saved.getId(), TicketChangedEvent.Trigger.CREATE);
        return ticketMapper.toDetail(saved);
    }

    @Transactional(readOnly = true)
    public TicketPage listTickets(String keyword, TicketStatus status, Pageable pageable) {
        String normalizedKeyword = keyword != null && keyword.isBlank() ? null : keyword;
        Specification<Ticket> spec = Specification.where(TicketSpecifications.matchesKeyword(normalizedKeyword))
                .and(TicketSpecifications.hasStatus(status));
        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        return new TicketPage(
                page.getContent().stream().map(ticketMapper::toSummary).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TicketDetail getTicket(UUID ticketId) {
        return ticketMapper.toDetail(findTicketWithComments(ticketId));
    }

    @Transactional
    public TicketDetail updateTicket(UUID ticketId, UpdateTicketRequest request) {
        Ticket ticket = findTicketWithComments(ticketId);
        if (request.title() != null) {
            ticket.setTitle(normalizeRequiredText(request.title(), "title"));
        }
        if (request.description() != null) {
            ticket.setDescription(request.description().trim());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignee() != null) {
            ticket.setAssignee(trimToNull(request.assignee()));
        }
        if (request.category() != null) {
            ticket.setCategory(request.category());
        }
        Ticket saved = ticketRepository.save(ticket);
        publishEvent(saved.getId(), TicketChangedEvent.Trigger.UPDATE);
        return ticketMapper.toDetail(saved);
    }

    @Transactional
    public TicketDetail transitionStatus(UUID ticketId, StatusTransitionRequest request) {
        Ticket ticket = findTicketWithComments(ticketId);
        TicketStateMachine.transition(ticket, request.status(), request.resolution());
        Ticket saved = ticketRepository.save(ticket);
        publishEvent(saved.getId(), TicketChangedEvent.Trigger.STATUS_CHANGE);
        return ticketMapper.toDetail(saved);
    }

    Ticket findTicketWithComments(UUID ticketId) {
        return ticketRepository
                .findByIdWithComments(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
    }

    private String generateDisplayId() {
        long sequence = ticketRepository.nextDisplayIdSequenceValue();
        return "TKT-" + sequence;
    }

    private static String normalizeRequiredText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(
                    field + " must not be blank",
                    List.of(new ValidationException.FieldViolation(field, field + " must not be blank")));
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void publishEvent(UUID ticketId, TicketChangedEvent.Trigger trigger) {
        eventPublisher.publishEvent(new TicketChangedEvent(ticketId, trigger));
    }
}
