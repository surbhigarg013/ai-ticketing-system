package com.ticketing.rag.ingestion;

import com.ticketing.rag.domain.IndexingJob;
import com.ticketing.rag.domain.IndexingJobStatus;
import com.ticketing.rag.domain.IndexingTrigger;
import com.ticketing.rag.repository.IndexingJobRepository;
import com.ticketing.ticket.event.TicketChangedEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IndexingJobEnqueuer {

    private final IndexingJobRepository indexingJobRepository;

    public IndexingJobEnqueuer(IndexingJobRepository indexingJobRepository) {
        this.indexingJobRepository = indexingJobRepository;
    }

    @Transactional
    public void enqueue(UUID ticketId, TicketChangedEvent.Trigger trigger) {
        if (indexingJobRepository.existsByTicketIdAndStatusIn(
                ticketId, List.of(IndexingJobStatus.PENDING, IndexingJobStatus.PROCESSING))) {
            return;
        }
        IndexingJob job = new IndexingJob();
        job.setTicketId(ticketId);
        job.setTrigger(toIndexingTrigger(trigger));
        job.setStatus(IndexingJobStatus.PENDING);
        indexingJobRepository.save(job);
    }

    private static IndexingTrigger toIndexingTrigger(TicketChangedEvent.Trigger trigger) {
        return IndexingTrigger.valueOf(trigger.name());
    }
}
