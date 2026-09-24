package com.ticketing.ticket.repository;

import com.ticketing.ticket.domain.Ticket;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByDisplayId(String displayId);

    @Query(value = "SELECT nextval('ticket_display_id_seq')", nativeQuery = true)
    long nextDisplayIdSequenceValue();

    @Query(
            """
            SELECT DISTINCT t FROM Ticket t
            LEFT JOIN FETCH t.comments
            WHERE t.id = :id
            """)
    Optional<Ticket> findByIdWithComments(@Param("id") UUID id);
}
