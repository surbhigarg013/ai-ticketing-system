package com.ticketing.rag.repository;

import com.ticketing.rag.domain.IndexingJob;
import com.ticketing.rag.domain.IndexingJobStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IndexingJobRepository extends JpaRepository<IndexingJob, UUID> {

    @Query(
            """
            SELECT j FROM IndexingJob j
            WHERE j.status = :status
            ORDER BY j.createdAt ASC
            """)
    List<IndexingJob> findByStatus(@Param("status") IndexingJobStatus status, Pageable pageable);

    boolean existsByTicketIdAndStatusIn(UUID ticketId, List<IndexingJobStatus> statuses);

    long countByTicketIdAndStatusIn(UUID ticketId, List<IndexingJobStatus> statuses);
}
