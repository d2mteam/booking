package com.booking.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SeatHeld(UUID reservationId, UUID sessionId, List<UUID> seatIds, Instant expiresAt) {
}
