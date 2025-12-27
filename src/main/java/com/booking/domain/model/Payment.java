package com.booking.domain.model;

import com.booking.domain.exception.BookingException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String provider;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    private Payment(UUID id, UUID reservationId, PaymentStatus status, BigDecimal amount, String currency, String provider, Instant createdAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.createdAt = createdAt;
    }

    public static Payment pending(UUID id, UUID reservationId, BigDecimal amount, String currency, String provider, Instant createdAt) {
        return new Payment(id, reservationId, PaymentStatus.PENDING, amount, currency, provider, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void succeed() {
        if (status == PaymentStatus.SUCCESS) {
            return;
        }
        if (status != PaymentStatus.PENDING) {
            throw new BookingException("Payment is not pending");
        }
        status = PaymentStatus.SUCCESS;
    }

    public void fail() {
        if (status == PaymentStatus.SUCCESS) {
            throw new BookingException("Payment already succeeded");
        }
        status = PaymentStatus.FAILED;
    }
}
