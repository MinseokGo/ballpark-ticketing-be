package com.ballpark.ticketing.reservation.dto;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.GameStatus;
import com.ballpark.ticketing.game.Winner;
import com.ballpark.ticketing.reservation.Reservation;
import com.ballpark.ticketing.reservation.ReservationStatus;
import java.time.LocalDateTime;

/** 내 예매 한 건. 경기 정보와 진행 상태를 같이 준다. */
public record MyReservationResponse(
		Long reservationId,
		Long gameId,
		String homeTeam,
		String awayTeam,
		LocalDateTime startAt,
		GameStatus gameStatus,
		GameProgress gameProgress,
		int homeScore,
		int awayScore,
		Winner winner,
		ReservationStatus status,
		long totalPrice,
		int seatCount,
		LocalDateTime createdAt) {

	public static MyReservationResponse from(Reservation reservation) {
		Game game = reservation.getGame();
		return new MyReservationResponse(
				reservation.getId(), game.getId(), game.getHomeTeam(), game.getAwayTeam(), game.getStartAt(),
				game.getStatus(), game.getProgress(), game.getHomeScore(), game.getAwayScore(), game.winner(),
				reservation.getStatus(), reservation.getTotalPrice(), reservation.getReservationSeats().size(),
				reservation.getCreatedAt());
	}
}
