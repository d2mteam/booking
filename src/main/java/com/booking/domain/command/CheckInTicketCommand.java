package com.booking.domain.command;

import java.util.UUID;

public record CheckInTicketCommand(UUID ticketId) {
}
