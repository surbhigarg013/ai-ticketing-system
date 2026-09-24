package com.ticketing.rag.ingestion;

import com.ticketing.ticket.event.TicketChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TicketChangedEventListener {

    private final IndexingJobEnqueuer indexingJobEnqueuer;

    public TicketChangedEventListener(IndexingJobEnqueuer indexingJobEnqueuer) {
        this.indexingJobEnqueuer = indexingJobEnqueuer;
    }

    @EventListener
    public void onTicketChanged(TicketChangedEvent event) {
        indexingJobEnqueuer.enqueue(event.ticketId(), event.trigger());
    }
}
