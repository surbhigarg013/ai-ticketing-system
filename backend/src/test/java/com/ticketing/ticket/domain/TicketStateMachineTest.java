package com.ticketing.ticket.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.shared.exception.InvalidStateTransitionException;
import com.ticketing.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TicketStateMachineTest {

    @Test
    void allowsOpenToInProgress() {
        assertThat(TicketStateMachine.canTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS))
                .isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = TicketStatus.class,
            names = {"OPEN", "IN_PROGRESS"},
            mode = EnumSource.Mode.EXCLUDE)
    void rejectsReopenFromTerminalStates(TicketStatus terminalStatus) {
        assertThat(TicketStateMachine.canTransition(terminalStatus, TicketStatus.OPEN)).isFalse();
    }

    @Test
    void requiresResolutionWhenTransitioningToResolved() {
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS);

        assertThatThrownBy(() -> TicketStateMachine.transition(ticket, TicketStatus.RESOLVED, "   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Resolution notes are required");
    }

    @Test
    void rejectsSkippedLifecycleStep() {
        Ticket ticket = ticket(TicketStatus.OPEN);

        assertThatThrownBy(() -> TicketStateMachine.transition(ticket, TicketStatus.RESOLVED, "Fixed"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void resolvedToClosedSucceeds() {
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS);
        TicketStateMachine.transition(ticket, TicketStatus.RESOLVED, "Fixed via backup gateway");

        TicketStateMachine.transition(ticket, TicketStatus.CLOSED, null);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getResolution()).isEqualTo("Fixed via backup gateway");
    }

    private static Ticket ticket(TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setStatus(status);
        ticket.setTitle("Payment timeout");
        ticket.setDescription("Customer card declined");
        ticket.setPriority(Priority.HIGH);
        ticket.setCategory(Category.PAYMENT);
        return ticket;
    }
}
