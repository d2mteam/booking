package com.booking.application;

import com.booking.domain.command.CheckInTicketCommand;
import com.booking.domain.command.ConfirmPaymentCommand;
import com.booking.domain.command.CreatePaymentCommand;
import com.booking.domain.command.ReleaseHoldCommand;
import com.booking.domain.command.ReleaseHoldReason;
import com.booking.domain.command.ReserveSeatCommand;
import com.booking.domain.exception.BookingException;
import com.booking.domain.model.Payment;
import com.booking.domain.model.enums.PaymentStatus;
import com.booking.domain.model.Reservation;
import com.booking.domain.model.enums.ReservationStatus;
import com.booking.domain.model.Seat;
import com.booking.domain.model.enums.SeatStatus;
import com.booking.domain.model.Session;
import com.booking.domain.model.Ticket;
import com.booking.domain.model.enums.TicketStatus;
import com.booking.infrastructure.HoldExpiryScheduler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final SessionRepository sessionRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final HoldExpiryScheduler holdExpiryScheduler;

    @Transactional
    public Reservation reserveSeats(ReserveSeatCommand command) {
        if (command.seatIds() == null || command.seatIds().isEmpty()) {
            throw new BookingException("Seat list is required");
        }
        if (command.holdDuration() == null || command.holdDuration().isNegative() || command.holdDuration().isZero()) {
            throw new BookingException("Hold duration is required");
        }
        Session session = sessionRepository.findByIdForUpdate(command.sessionId())
                .orElseThrow(() -> new BookingException("Session not found"));
        if (!session.isOpen()) {
            throw new BookingException("Session is closed");
        }

        List<Seat> seats = seatRepository.findAllBySessionIdAndIdInForUpdate(session.getId(), command.seatIds());
        if (seats.size() != command.seatIds().size()) {
            throw new BookingException("Seat not found");
        }
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new BookingException("Seat not available");
            }
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(command.holdDuration());
        Reservation reservation = Reservation.active(UUID.randomUUID(), command.userId(), session, expiresAt, now);
        reservationRepository.save(reservation);

        for (Seat seat : seats) {
            seat.hold(reservation.getId(), expiresAt);
        }
        seatRepository.saveAll(seats);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                holdExpiryScheduler.scheduleHoldExpiry(reservation.getId(), reservation.getExpiresAt());
            }
        });

        return reservation;
    }

    @Transactional
    public void releaseHold(ReleaseHoldCommand command) {
        Reservation reservation = reservationRepository.findByIdForUpdate(command.reservationId())
                .orElseThrow(() -> new BookingException("Reservation not found"));
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            return;
        }

        Instant now = Instant.now();
        if (command.reason() == ReleaseHoldReason.EXPIRE && reservation.getExpiresAt().isAfter(now)) {
            return;
        }

        List<Seat> seats = seatRepository.findAllByHoldIdForUpdate(reservation.getId());
        for (Seat seat : seats) {
            seat.releaseHold();
        }
        seatRepository.saveAll(seats);
        reservation.expire();
        reservationRepository.save(reservation);
    }

    @Transactional
    public Payment createPayment(CreatePaymentCommand command) {
        Reservation reservation = reservationRepository.findByIdForUpdate(command.reservationId())
                .orElseThrow(() -> new BookingException("Reservation not found"));
        if (reservation.getStatus() != ReservationStatus.ACTIVE || reservation.isExpired(Instant.now())) {
            throw new BookingException("Reservation is not active");
        }
        Payment payment = Payment.pending(
                command.paymentId(),
                reservation.getId(),
                command.amount(),
                command.currency(),
                command.provider(),
                Instant.now()
        );
        return paymentRepository.save(payment);
    }

    @Transactional
    public Ticket confirmPayment(ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findByIdForUpdate(command.paymentId())
                .orElseThrow(() -> new BookingException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return ticketRepository.findByReservationId(payment.getReservationId()).orElse(null);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BookingException("Payment is not pending");
        }

        Reservation reservation = reservationRepository.findByIdForUpdate(payment.getReservationId())
                .orElseThrow(() -> new BookingException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.ACTIVE || reservation.isExpired(Instant.now())) {
            payment.fail();
            paymentRepository.save(payment);
            return null;
        }

        List<Seat> seats = seatRepository.findAllByHoldIdForUpdate(reservation.getId());
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.HELD) {
                throw new BookingException("Seat is not held");
            }
            seat.sell();
        }
        seatRepository.saveAll(seats);

        reservation.consume();
        reservationRepository.save(reservation);

        Ticket ticket = Ticket.issue(
                UUID.randomUUID(),
                reservation.getId(),
                reservation.getSession().getId(),
                reservation.getUserId(),
                "QR-" + UUID.randomUUID(),
                Instant.now()
        );
        ticketRepository.save(ticket);

        payment.succeed();
        paymentRepository.save(payment);

        return ticket;
    }

    @Transactional
    public Ticket checkIn(CheckInTicketCommand command) {
        Ticket ticket = ticketRepository.findByIdForUpdate(command.ticketId())
                .orElseThrow(() -> new BookingException("Ticket not found"));
        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new BookingException("Ticket is not valid");
        }

        Session session = sessionRepository.findById(ticket.getSessionId())
                .orElseThrow(() -> new BookingException("Session not found"));
        if (session.getEndsAt().isBefore(Instant.now())) {
            throw new BookingException("Session already ended");
        }

        ticket.use();
        return ticketRepository.save(ticket);
    }
}
