package com.ticketing.ticket.domain;

import com.ticketing.shared.exception.InvalidStateTransitionException;
import com.ticketing.shared.exception.ValidationException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TicketStateMachine {

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = Map.of(
            TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
            TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED),
            TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class),
            TicketStatus.CANCELLED, EnumSet.noneOf(TicketStatus.class));

    private TicketStateMachine() {}

    public static boolean canTransition(TicketStatus from, TicketStatus to) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TicketStatus.class)).contains(to);
    }

    public static TicketStatus transition(Ticket ticket, TicketStatus target, String resolution) {
        TicketStatus current = ticket.getStatus();
        if (!canTransition(current, target)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition from " + current + " to " + target);
        }
        if (target == TicketStatus.RESOLVED && !hasNonBlankResolution(resolution)) {
            throw new ValidationException(
                    "Resolution notes are required when transitioning to RESOLVED");
        }
        if (target == TicketStatus.RESOLVED) {
            ticket.setResolution(resolution.trim());
        }
        ticket.setStatus(target);
        return target;
    }

    private static boolean hasNonBlankResolution(String resolution) {
        return resolution != null && !resolution.trim().isEmpty();
    }
}
