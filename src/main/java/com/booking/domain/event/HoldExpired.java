package com.booking.domain.event;

import java.util.UUID;

public record HoldExpired(UUID reservationId) {
}
