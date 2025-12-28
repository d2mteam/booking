package com.booking.domain.model;

import com.booking.domain.exception.BookingException;
import com.booking.domain.model.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import lombok.*;

@Data
@Entity
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Reservation {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Reservation active(UUID id, UUID userId, Session session, Instant expiresAt, Instant createdAt) {
        return new Reservation(id, userId, session, ReservationStatus.ACTIVE, expiresAt, createdAt);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void consume() {
        if (status != ReservationStatus.ACTIVE) {
            throw new BookingException("Reservation is not active");
        }
        status = ReservationStatus.CONSUMED;
    }

    public void expire() {
        if (status != ReservationStatus.ACTIVE) {
            return;
        }
        status = ReservationStatus.EXPIRED;
    }
}
