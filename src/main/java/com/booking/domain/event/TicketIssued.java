package com.booking.domain.event;

import java.util.UUID;

public record TicketIssued(UUID ticketId, UUID reservationId) {
}
