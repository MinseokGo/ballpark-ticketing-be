package com.ballpark.ticketing.reservation.dto;

import com.ballpark.ticketing.reservation.Reservation;
import com.ballpark.ticketing.reservation.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ReservationResponse(
		Long id,
		Long userId,
		Long gameId,
		ReservationStatus status,
		long totalPrice,
		List<Long> gameSeatIds,
		LocalDateTime createdAt) {

	public static ReservationResponse from(Reservation reservation) {
		List<Long> gameSeatIds = reservation.getReservationSeats().stream()
				.map(reservationSeat -> reservationSeat.getGameSeat().getId())
				.toList();
		return new ReservationResponse(
				reservation.getId(), reservation.getUserId(), reservation.getGame().getId(),
				reservation.getStatus(), reservation.getTotalPrice(), gameSeatIds, reservation.getCreatedAt());
	}
}
