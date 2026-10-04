package com.ballpark.ticketing.reservation.repository;

import com.ballpark.ticketing.reservation.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
