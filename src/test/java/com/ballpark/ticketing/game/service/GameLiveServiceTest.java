package com.ballpark.ticketing.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.Half;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.dto.LiveEventResponse;
import com.ballpark.ticketing.game.dto.LiveStateResponse;
import com.ballpark.ticketing.game.repository.PlayerRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class GameLiveServiceTest {

	@Autowired
	private GameService gameService;

	@Autowired
	private GameLiveService gameLiveService;

	@Autowired
	private PlayerRepository playerRepository;

	private GameResponse createGame() {
		return gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2020, 11, 1, 18, 30), LocalDateTime.of(2020, 10, 25, 11, 0)));
	}

	@Test
	void numbersEventsFromOneAndReplaysOnlyTheLaterOnes() {
		GameResponse game = createGame();
		gameLiveService.record(game.id(), new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null));
		gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.SCORE_CHANGED, null, null, 1, 0));
		gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.INNING_CHANGED, 2, Half.TOP, null, null));

		List<LiveEventResponse> replay = gameLiveService.eventsAfter(game.id(), 1);

		assertThat(replay).extracting(LiveEventResponse::seq).containsExactly(2, 3);
		assertThat(replay.get(1).inning()).isEqualTo(2);
	}

	@Test
	void snapshotReflectsTheLatestState() {
		GameResponse game = createGame();
		gameLiveService.record(game.id(), new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null));
		gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.SCORE_CHANGED, null, null, 2, 1));

		LiveStateResponse state = gameLiveService.snapshot(game.id());

		assertThat(state.progress()).isEqualTo(GameProgress.LIVE);
		assertThat(state.homeScore()).isEqualTo(2);
		assertThat(state.awayScore()).isEqualTo(1);
		assertThat(state.seq()).isEqualTo(2);
	}

	@Test
	void finishingAGameEndsTheLog() {
		GameResponse game = createGame();
		gameLiveService.record(game.id(), new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null));
		LiveEventResponse finished = gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.GAME_FINISHED, null, null, 5, 3));

		assertThat(finished.isTerminal()).isTrue();
		assertThatThrownBy(() -> gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.SCORE_CHANGED, null, null, 6, 3)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_ALREADY_ENDED);
	}

	@Test
	void rejectsAnEventWithoutItsRequiredValues() {
		GameResponse game = createGame();
		gameLiveService.record(game.id(), new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null));

		assertThatThrownBy(() -> gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.SCORE_CHANGED, null, null, null, 0)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	void unknownGameIsNotFound() {
		assertThatThrownBy(() -> gameLiveService.snapshot(999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_NOT_FOUND);
	}

	private Long rosterPlayerId(String team) {
		return playerRepository.findByTeamNameOrderByBackNumberAsc(team).get(0).getId();
	}

	@Test
	void recordsAPlayWithItsPlayerAndKind() {
		GameResponse game = gameService.create(new GameCreateRequest(
				"두산 베어스", "LG 트윈스",
				LocalDateTime.of(2020, 11, 1, 18, 30), LocalDateTime.of(2020, 10, 25, 11, 0)));
		gameLiveService.record(game.id(), new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null));
		Long batter = rosterPlayerId("두산 베어스");
		Long pitcher = rosterPlayerId("LG 트윈스");

		LiveEventResponse play = gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.PLAY, null, null, null, null, batter, "STRIKEOUT", pitcher));

		assertThat(play.detail()).isEqualTo("STRIKEOUT");
		assertThat(play.playerId()).isEqualTo(batter);
		assertThat(play.secondaryPlayerId()).isEqualTo(pitcher);
	}

	@Test
	void rejectsAPlayWithoutAKnownKind() {
		GameResponse game = gameService.create(new GameCreateRequest(
				"두산 베어스", "LG 트윈스",
				LocalDateTime.of(2020, 11, 1, 18, 30), LocalDateTime.of(2020, 10, 25, 11, 0)));
		gameLiveService.record(game.id(), new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null));
		Long batter = rosterPlayerId("두산 베어스");

		assertThatThrownBy(() -> gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.PLAY, null, null, null, null, batter, "NOT_A_PLAY", null)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
	}
}
