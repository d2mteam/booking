package com.booking.domain.model;

import com.booking.domain.exception.BookingException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    private UUID id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "qr_code", nullable = false)
    private String qrCode;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected Ticket() {
    }

    private Ticket(UUID id, UUID reservationId, UUID sessionId, UUID userId, TicketStatus status, String qrCode, Instant issuedAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = status;
        this.qrCode = qrCode;
        this.issuedAt = issuedAt;
    }

    public static Ticket issue(UUID id, UUID reservationId, UUID sessionId, UUID userId, String qrCode, Instant issuedAt) {
        return new Ticket(id, reservationId, sessionId, userId, TicketStatus.VALID, qrCode, issuedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getQrCode() {
        return qrCode;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void use() {
        if (status != TicketStatus.VALID) {
            throw new BookingException("Ticket is not valid for check-in");
        }
        status = TicketStatus.USED;
    }
}
