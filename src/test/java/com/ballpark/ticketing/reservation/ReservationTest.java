package com.ballpark.ticketing.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.Seat;
import com.ballpark.ticketing.game.Section;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationTest {

	private final Game game = new Game("Seoul Comets", "Busan Gulls",
			LocalDateTime.of(2026, 10, 10, 18, 30), LocalDateTime.of(2026, 10, 3, 11, 0));
	private final GameSeat gameSeat = new GameSeat(game, new Seat(new Section("Infield 101", "R", 30_000), 1, 1));

	@Test
	void startsPendingAndCanBeConfirmed() {
		Reservation reservation = new Reservation(1L, game, List.of(gameSeat), 30_000);

		reservation.confirm();

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(reservation.getReservationSeats()).hasSize(1);
	}

	@Test
	void rejectsConfirmingACancelledReservation() {
		Reservation reservation = new Reservation(1L, game, List.of(gameSeat), 30_000);
		reservation.cancel();

		assertThatThrownBy(reservation::confirm)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_NOT_PENDING);
	}

	@Test
	void rejectsAReservationWithoutSeats() {
		assertThatThrownBy(() -> new Reservation(1L, game, List.of(), 0))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.RESERVATION_SEATS_EMPTY);
	}
}
