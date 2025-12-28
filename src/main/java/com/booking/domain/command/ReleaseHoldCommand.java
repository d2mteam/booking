package com.booking.domain.command;

import java.util.UUID;

public record ReleaseHoldCommand(UUID reservationId, ReleaseHoldReason reason) {
}
