package com.ballpark.ticketing.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;

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
	void cannotStartBeforeStartAt() {
		Game game = new Game("Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 10, 10, 18, 30), LocalDateTime.of(2026, 10, 3, 11, 0));

		assertThatThrownBy(() -> game.start(LocalDateTime.of(2026, 10, 10, 18, 29)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.GAME_NOT_YET_STARTABLE);
		assertThat(game.getProgress()).isEqualTo(GameProgress.NOT_STARTED);
	}

	@Test
	void startedAtAndAfterStartAt() {
		assertThat(game.hasStarted(LocalDateTime.of(2026, 10, 10, 18, 30))).isTrue();
		assertThat(game.hasStarted(LocalDateTime.of(2026, 10, 11, 0, 0))).isTrue();
	}

	private Game liveGame() {
		Game game = new Game("Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 10, 10, 18, 30), LocalDateTime.of(2026, 10, 3, 11, 0));
		game.start(LocalDateTime.of(2026, 10, 10, 18, 30));
		return game;
	}

	@Test
	void startsOnceAtTheTopOfFirstInning() {
		Game game = liveGame();

		assertThat(game.getProgress()).isEqualTo(GameProgress.LIVE);
		assertThat(game.getInning()).isEqualTo(1);
		assertThat(game.getHalf()).isEqualTo(Half.TOP);
		assertThatThrownBy(() -> game.start(LocalDateTime.of(2026, 10, 10, 18, 30)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.GAME_ALREADY_STARTED);
	}

	@Test
	void inningOnlyMovesForward() {
		Game game = liveGame();

		game.changeInning(1, Half.BOTTOM);
		assertThat(game.getHalf()).isEqualTo(Half.BOTTOM);
		assertThatThrownBy(() -> game.changeInning(1, Half.TOP))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INNING_NOT_ADVANCED);
	}

	@Test
	void scoreCannotDecreaseButCanBeCorrected() {
		Game game = liveGame();
		game.advanceScore(3, 2);

		assertThatThrownBy(() -> game.advanceScore(2, 2))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.SCORE_DECREASED);
		game.correctScore(2, 2);
		assertThat(game.getHomeScore()).isEqualTo(2);
	}

	@Test
	void finishingFixesTheWinner() {
		Game game = liveGame();

		game.finish(5, 3);

		assertThat(game.getProgress()).isEqualTo(GameProgress.FINISHED);
		assertThat(game.winner()).isEqualTo(Winner.HOME);
	}

	@Test
	void aTieIsADraw() {
		Game game = liveGame();

		game.finish(4, 4);

		assertThat(game.winner()).isEqualTo(Winner.DRAW);
	}

	@Test
	void noWinnerBeforeTheGameFinishes() {
		assertThat(liveGame().winner()).isNull();
	}

	@Test
	void endedGamesRejectEvents() {
		Game game = liveGame();
		game.finish(1, 0);

		assertThatThrownBy(() -> game.advanceScore(2, 0))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.GAME_ALREADY_ENDED);
		assertThatThrownBy(game::cancel)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.GAME_ALREADY_ENDED);
	}

	@Test
	void eventsNeedALiveGame() {
		Game game = new Game("Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 10, 10, 18, 30), LocalDateTime.of(2026, 10, 3, 11, 0));

		assertThatThrownBy(() -> game.advanceScore(1, 0))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.GAME_NOT_LIVE);
	}

	@Test
	void numbersEventsInOrder() {
		Game game = liveGame();

		assertThat(game.nextEventSeq()).isEqualTo(1);
		assertThat(game.nextEventSeq()).isEqualTo(2);
	}
}
