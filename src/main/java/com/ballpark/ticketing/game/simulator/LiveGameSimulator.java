package com.ballpark.ticketing.game.simulator;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.Half;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.dto.LiveStateResponse;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.service.GameLiveService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.random.RandomGenerator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 데모용 중계 생성기. 진행 중 경기를 일정하게 유지하면서 득점·이닝 이벤트를 랜덤으로 만든다.
 * 모든 이벤트는 관리자 API와 같은 경로(GameLiveService)로 들어가서, DB 기록과 SSE 전송이 실제 입력과 같다.
 * app.live-simulator.enabled가 true일 때만 돈다(local 프로필에서 켠다).
 */
@Slf4j
@Component
public class LiveGameSimulator {

	static final int TARGET_LIVE_GAMES = 3;
	static final int FORCED_END_INNING = 12;
	private static final int REGULAR_INNINGS = 9;
	private static final double SCORE_PROBABILITY = 0.30;
	private static final double HALF_INNING_END_PROBABILITY = 0.10;

	private final GameRepository gameRepository;
	private final GameLiveService gameLiveService;
	private final Clock clock;
	private final boolean enabled;
	private final RandomGenerator random = RandomGenerator.getDefault();

	public LiveGameSimulator(
			GameRepository gameRepository,
			GameLiveService gameLiveService,
			Clock clock,
			@Value("${app.live-simulator.enabled:false}") boolean enabled) {
		this.gameRepository = gameRepository;
		this.gameLiveService = gameLiveService;
		this.clock = clock;
		this.enabled = enabled;
	}

	@Scheduled(fixedDelay = 3_000)
	public void scheduledTick() {
		if (enabled) {
			tick(random);
		}
	}

	/** 한 번 돈다: 진행 중 경기가 모자라면 예정 경기를 시작하고, 각 진행 중 경기에 랜덤 이벤트를 하나씩 준다. */
	public void tick(RandomGenerator random) {
		fillLiveGames();
		for (Long gameId : liveGameIds()) {
			try {
				advance(gameLiveService.snapshot(gameId), random);
			} catch (RuntimeException error) {
				// 한 경기의 실패가 다른 경기 중계를 멈추지 않게 한다.
				log.warn("[simulator] 경기 {} 이벤트 기록 실패: {}", gameId, error.getMessage());
			}
		}
	}

	private void fillLiveGames() {
		int missing = TARGET_LIVE_GAMES - liveGameIds().size();
		if (missing <= 0) {
			return;
		}
		gameRepository.findByProgress(GameProgress.NOT_STARTED,
						PageRequest.of(0, missing, Sort.by("startAt")))
				.forEach(game -> {
					gameLiveService.record(game.getId(),
							new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null));
					log.info("[simulator] 경기 {} 시작", game.getId());
				});
	}

	/**
	 * 중계를 돌리는 진행 중 경기. 예정 시각이 아직 오지 않은 경기는 뺀다.
	 * 데이터 테스트용으로 미래 경기를 진행 중으로 두면, 이 생성기가 몇 분 안에 끝내 버리지 않게 하려는 것이다.
	 */
	private List<Long> liveGameIds() {
		LocalDateTime now = LocalDateTime.now(clock);
		return gameRepository.findByProgress(GameProgress.LIVE, Pageable.unpaged())
				.stream()
				.filter(game -> !game.getStartAt().isAfter(now))
				.map(Game::getId)
				.toList();
	}

	void advance(LiveStateResponse state, RandomGenerator random) {
		if (state.progress() != GameProgress.LIVE) {
			return;
		}
		double roll = random.nextDouble();
		if (roll < SCORE_PROBABILITY) {
			scoreOnce(state, random);
		} else if (roll < SCORE_PROBABILITY + HALF_INNING_END_PROBABILITY) {
			endHalfInning(state);
		}
	}

	private void scoreOnce(LiveStateResponse state, RandomGenerator random) {
		boolean homeScores = random.nextBoolean();
		int home = state.homeScore() + (homeScores ? 1 : 0);
		int away = state.awayScore() + (homeScores ? 0 : 1);
		gameLiveService.record(state.gameId(),
				new LiveEventCreateRequest(GameEventType.SCORE_CHANGED, null, null, home, away));
	}

	private void endHalfInning(LiveStateResponse state) {
		boolean bottom = state.half() == Half.BOTTOM;
		int inning = state.inning();
		if (bottom && inning >= REGULAR_INNINGS && state.homeScore() != state.awayScore()) {
			gameLiveService.record(state.gameId(),
					new LiveEventCreateRequest(GameEventType.GAME_FINISHED, null, null, state.homeScore(), state.awayScore()));
			return;
		}
		if (bottom && inning >= FORCED_END_INNING) {
			gameLiveService.record(state.gameId(),
					new LiveEventCreateRequest(GameEventType.GAME_FINISHED, null, null, state.homeScore(), state.awayScore()));
			return;
		}
		if (bottom) {
			gameLiveService.record(state.gameId(),
					new LiveEventCreateRequest(GameEventType.INNING_CHANGED, inning + 1, Half.TOP, null, null));
		} else {
			gameLiveService.record(state.gameId(),
					new LiveEventCreateRequest(GameEventType.INNING_CHANGED, inning, Half.BOTTOM, null, null));
		}
	}
}
