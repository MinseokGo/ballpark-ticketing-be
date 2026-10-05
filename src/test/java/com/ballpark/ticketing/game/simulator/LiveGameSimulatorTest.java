package com.ballpark.ticketing.game.simulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.service.GameLiveService;
import com.ballpark.ticketing.game.service.GameService;
import java.util.random.RandomGenerator;
import java.time.LocalDateTime;
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
class LiveGameSimulatorTest {

	@Autowired
	private LiveGameSimulator simulator;

	@Autowired
	private GameService gameService;

	@Autowired
	private GameLiveService gameLiveService;

	@Autowired
	private GameRepository gameRepository;

	@Test
	@Transactional
	void keepsRandomEventsFlowingAndNeverBreaksTheRules() {
		GameResponse game = gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 10, 1, 18, 30), LocalDateTime.of(2026, 9, 25, 11, 0)));
		// 생성기는 예정 종료 시각이 있는 경기만 돌린다. 테스트 동안 끝나지 않도록 먼 미래로 둔다.
		gameRepository.findById(game.id()).orElseThrow().planEnd(LocalDateTime.of(2030, 1, 1, 0, 0));
		gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.GAME_STARTED, null, null, null, null));

		RandomGenerator random = RandomGenerator.of("L64X128MixRandom");
		for (int i = 0; i < 1500; i++) {
			simulator.tick(random);
		}

		var state = gameLiveService.snapshot(game.id());
		assertThat(state.seq()).isGreaterThan(1);
		assertThat(state.progress()).isIn(GameProgress.LIVE, GameProgress.FINISHED);
		assertThat(gameLiveService.eventsAfter(game.id(), 0))
				.extracting(event -> event.seq())
				.isSorted();
	}

	@Test
	void neverStartsOrDrivesAGameBeforeItsStartTime() {
		GameResponse game = gameService.create(new GameCreateRequest(
				"NC Dinos", "KT Wiz",
				LocalDateTime.of(2027, 10, 5, 18, 30), LocalDateTime.of(2027, 9, 1, 11, 0)));

		assertThatThrownBy(() -> gameLiveService.record(game.id(),
				new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.GAME_NOT_YET_STARTABLE);

		RandomGenerator random = RandomGenerator.of("L64X128MixRandom");
		for (int i = 0; i < 100; i++) {
			simulator.tick(random);
		}

		assertThat(gameLiveService.snapshot(game.id()).seq()).isZero();
		assertThat(gameLiveService.snapshot(game.id()).progress()).isEqualTo(GameProgress.NOT_STARTED);
	}
}
