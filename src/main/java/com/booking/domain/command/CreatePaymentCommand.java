package com.booking.domain.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentCommand(
        UUID reservationId,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String provider
) {
}
