package com.booking.infrastructure;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class HoldExpiry implements Delayed {

    private final UUID reservationId;
    private final Instant expiresAt;

    public HoldExpiry(UUID reservationId, Instant expiresAt) {
        this.reservationId = reservationId;
        this.expiresAt = expiresAt;
    }

    public UUID getReservationId() {
        return reservationId;
    }

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
