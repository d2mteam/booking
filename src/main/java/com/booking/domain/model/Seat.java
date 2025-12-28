package com.booking.domain.model;

import com.booking.domain.exception.BookingException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "seat_no"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Seat {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "seat_no", nullable = false)
    private String seatNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Column(name = "hold_id")
    private UUID holdId;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;


    public void hold(UUID holdId, Instant expiresAt) {
        if (status != SeatStatus.AVAILABLE) {
            throw new BookingException("Seat is not available");
        }
        this.status = SeatStatus.HELD;
        this.holdId = holdId;
        this.holdExpiresAt = expiresAt;
    }

    public void releaseHold() {
        if (status != SeatStatus.HELD) {
            return;
        }
        this.status = SeatStatus.AVAILABLE;
        this.holdId = null;
        this.holdExpiresAt = null;
    }

    public void sell() {
        if (status != SeatStatus.HELD) {
            throw new BookingException("Seat must be held before selling");
        }
        this.status = SeatStatus.SOLD;
    }
}
