package com.ballpark.ticketing.game.simulator;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
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

	@Test
	void keepsRandomEventsFlowingAndNeverBreaksTheRules() {
		GameResponse game = gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0)));
		gameLiveService.record(game.id(), new LiveEventCreateRequest(
				GameEventType.GAME_STARTED, null, null, null, null));

		RandomGenerator random = RandomGenerator.of("L64X128MixRandom");
		for (int i = 0; i < 200; i++) {
			simulator.tick(random);
		}

		var state = gameLiveService.snapshot(game.id());
		assertThat(state.seq()).isGreaterThan(1);
		assertThat(state.progress()).isIn(GameProgress.LIVE, GameProgress.FINISHED);
		assertThat(gameLiveService.eventsAfter(game.id(), 0))
				.extracting(event -> event.seq())
				.isSorted();
	}
}
