package com.ballpark.ticketing.reservation.repository;

import com.ballpark.ticketing.reservation.Reservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	List<Reservation> findByUserIdOrderByIdDesc(Long userId);
}
