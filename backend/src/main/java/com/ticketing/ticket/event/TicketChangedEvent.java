package com.ticketing.ticket.event;

import java.util.UUID;

public record TicketChangedEvent(UUID ticketId, Trigger trigger) {

    public enum Trigger {
        CREATE,
        UPDATE,
        COMMENT_ADD,
        COMMENT_UPDATE,
        COMMENT_DELETE,
        STATUS_CHANGE
    }
}
