package com.booking.domain.command;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public record ReserveSeatCommand(
        UUID userId,
        UUID sessionId,
        List<UUID> seatIds,
        Duration holdDuration
) {
}
