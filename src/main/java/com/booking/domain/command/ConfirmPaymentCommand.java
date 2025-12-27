package com.booking.domain.command;

import java.util.UUID;

public record ConfirmPaymentCommand(UUID paymentId) {
}
