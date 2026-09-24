package com.ticketing.ticket.repository;

import com.ticketing.ticket.domain.Ticket;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByDisplayId(String displayId);

    @Query(value = "SELECT nextval('ticket_display_id_seq')", nativeQuery = true)
    long nextDisplayIdSequenceValue();
}
