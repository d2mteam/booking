package com.booking.domain.event;

import java.util.UUID;

public record PaymentSucceeded(UUID paymentId, UUID reservationId) {
}
