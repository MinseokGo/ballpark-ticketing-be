package com.ballpark.ticketing.reservation.repository;

import com.ballpark.ticketing.reservation.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {

	/**
	 * 취소되지 않은 예약만 센다. 1인 경기당 최대 4매 제한(#1 경기 일치 검증과 별개)에 쓴다.
	 */
	@Query("""
			select count(rs)
			from ReservationSeat rs
			where rs.reservation.userId = :userId
				and rs.reservation.game.id = :gameId
				and rs.reservation.status <> com.ballpark.ticketing.reservation.ReservationStatus.CANCELLED
			""")
	long countActiveByUserAndGame(@Param("userId") Long userId, @Param("gameId") Long gameId);
}
