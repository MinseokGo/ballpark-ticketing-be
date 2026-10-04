package com.ballpark.ticketing.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GameSeatTest {

	private final Game game = new Game("Seoul Comets", "Busan Gulls",
			LocalDateTime.of(2026, 10, 10, 18, 30), LocalDateTime.of(2026, 10, 3, 11, 0));
	private final Seat seat = new Seat(new Section("Infield 101", "R", 30_000), 1, 1);

	@Test
	void holdsThenSellsAnAvailableSeat() {
		GameSeat gameSeat = new GameSeat(game, seat);

		gameSeat.hold();
		gameSeat.sell();

		assertThat(gameSeat.getStatus()).isEqualTo(GameSeatStatus.SOLD);
	}

	@Test
	void rejectsHoldingASeatThatIsAlreadyHeld() {
		GameSeat gameSeat = new GameSeat(game, seat);
		gameSeat.hold();

		assertThatThrownBy(gameSeat::hold).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejectsSellingASeatThatWasNotHeld() {
		GameSeat gameSeat = new GameSeat(game, seat);

		assertThatThrownBy(gameSeat::sell).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void releasesAHeldSeat() {
		GameSeat gameSeat = new GameSeat(game, seat);
		gameSeat.hold();

		gameSeat.release();

		assertThat(gameSeat.getStatus()).isEqualTo(GameSeatStatus.AVAILABLE);
	}
}
