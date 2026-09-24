package com.ticketing.ticket.api;

import com.ticketing.ticket.domain.Comment;
import com.ticketing.ticket.domain.Ticket;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    TicketSummary toSummary(Ticket ticket);

    @Mapping(target = "comments", expression = "java(sortedComments(ticket))")
    TicketDetail toDetail(Ticket ticket);

    CommentDto toCommentDto(Comment comment);

    default List<CommentDto> sortedComments(Ticket ticket) {
        return ticket.getComments().stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .map(this::toCommentDto)
                .toList();
    }
}
