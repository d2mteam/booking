package com.booking.infrastructure;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public record HoldExpiry(UUID reservationId, Instant expiresAt) implements Delayed {

    @Override
    public long getDelay(TimeUnit unit) {
        long delayMillis = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other == this) {
            return 0;
        }
        long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
        return Long.compare(diff, 0);
    }
}
