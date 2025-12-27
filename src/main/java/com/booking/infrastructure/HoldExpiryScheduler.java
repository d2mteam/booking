package com.booking.infrastructure;

import com.booking.application.BookingService;
import com.booking.domain.command.ReleaseHoldCommand;
import com.booking.domain.command.ReleaseHoldReason;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HoldExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HoldExpiryScheduler.class);

    private final BookingService bookingService;
    private final DelayQueue<HoldExpiry> queue = new DelayQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HoldExpiryScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public void scheduleHoldExpiry(java.util.UUID reservationId, Instant expiresAt) {
        queue.offer(new HoldExpiry(reservationId, expiresAt));
    }

    @PostConstruct
    public void start() {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    HoldExpiry expiry = queue.take();
                    bookingService.releaseHold(new ReleaseHoldCommand(expiry.getReservationId(), ReleaseHoldReason.EXPIRE));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    logger.warn("Failed to expire hold", ex);
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
