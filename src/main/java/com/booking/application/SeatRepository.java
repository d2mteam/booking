package com.booking.application;

import com.booking.domain.model.Seat;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.session.id = :sessionId and s.id in :seatIds")
    List<Seat> findAllBySessionIdAndIdInForUpdate(
            @Param("sessionId") UUID sessionId,
            @Param("seatIds") List<UUID> seatIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.holdId = :holdId")
    List<Seat> findAllByHoldIdForUpdate(@Param("holdId") UUID holdId);
}
