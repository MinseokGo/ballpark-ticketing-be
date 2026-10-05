package com.ballpark.ticketing.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GameTest {

	private final Game game = new Game("Seoul Comets", "Busan Gulls",
			LocalDateTime.of(2026, 10, 10, 18, 30), LocalDateTime.of(2026, 10, 3, 11, 0));

	@Test
	void notStartedBeforeStartAt() {
		assertThat(game.hasStarted(LocalDateTime.of(2026, 10, 10, 18, 29))).isFalse();
	}

	@Test
	void startedAtAndAfterStartAt() {
		assertThat(game.hasStarted(LocalDateTime.of(2026, 10, 10, 18, 30))).isTrue();
		assertThat(game.hasStarted(LocalDateTime.of(2026, 10, 11, 0, 0))).isTrue();
	}
}
