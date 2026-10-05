package com.ballpark.ticketing.game.simulator;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.Player;
import com.ballpark.ticketing.game.PlayKind;
import com.ballpark.ticketing.game.Half;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.dto.LiveStateResponse;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.repository.PlayerRepository;
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

	static final int FORCED_END_INNING = 12;
	private static final int REGULAR_INNINGS = 9;
	// 2초마다 한 번 도는 기준으로 맞춘 실제 속도. 득점은 5~8분에 한 번, 이닝 교체는 10분 안팎, 플레이는 1~2분에 한 번꼴이다.
	private static final double SCORE_PROBABILITY = 0.005;
	private static final double HALF_INNING_END_PROBABILITY = 0.0033;
	private static final double PLAY_PROBABILITY = 0.03;
	private static final List<PlayKind> SCORING_PLAYS = List.of(
			PlayKind.HIT, PlayKind.DOUBLE, PlayKind.HOME_RUN, PlayKind.SACRIFICE_FLY, PlayKind.WILD_PITCH);
	// 득점 외 일반 플레이. 삼진·볼넷·병살·도루 등 기록 화면에서 보여준다.
	private static final List<PlayKind> PLAYS = List.of(
			PlayKind.STRIKEOUT, PlayKind.STRIKEOUT, PlayKind.WALK, PlayKind.GROUND_OUT, PlayKind.FLY_OUT,
			PlayKind.DOUBLE_PLAY, PlayKind.STOLEN_BASE, PlayKind.CAUGHT_STEALING, PlayKind.HIT_BY_PITCH,
			PlayKind.ERROR, PlayKind.HIT, PlayKind.DOUBLE);
	// 투수가 함께 기록되는 플레이
	private static final java.util.Set<PlayKind> PITCHER_PLAYS = java.util.EnumSet.of(
			PlayKind.STRIKEOUT, PlayKind.WALK, PlayKind.HIT_BY_PITCH, PlayKind.GROUND_OUT, PlayKind.FLY_OUT,
			PlayKind.DOUBLE_PLAY);
	private final GameRepository gameRepository;
	private final GameLiveService gameLiveService;
	private final PlayerRepository playerRepository;
	private final Clock clock;
	private final boolean enabled;
	private final double speed;
	private final RandomGenerator random = RandomGenerator.getDefault();

	public LiveGameSimulator(
			GameRepository gameRepository,
			GameLiveService gameLiveService,
			PlayerRepository playerRepository,
			Clock clock,
			@Value("${app.live-simulator.enabled:false}") boolean enabled,
			@Value("${app.live-simulator.speed:1.0}") double speed) {
		this.gameRepository = gameRepository;
		this.gameLiveService = gameLiveService;
		this.playerRepository = playerRepository;
		this.clock = clock;
		this.enabled = enabled;
		this.speed = speed;
	}

	@Scheduled(fixedDelay = 2_000)
	public void scheduledTick() {
		if (enabled) {
			tick(random);
		}
	}

	/** 한 번 돈다: 시작 시각이 된 경기를 시작하고, 진행 중 경기마다 이벤트를 한 번 줄 수 있다. 예정 종료가 지나면 끝낸다. */
	public void tick(RandomGenerator random) {
		LocalDateTime now = LocalDateTime.now(clock);
		startDueGames(now);
		for (Long gameId : liveGameIds()) {
			try {
				Game game = gameRepository.findById(gameId).orElseThrow();
				LiveStateResponse state = gameLiveService.snapshot(gameId);
				if (game.isPastPlannedEnd(now)) {
					finish(state);
				} else {
					advance(state, random);
				}
			} catch (RuntimeException error) {
				// 한 경기의 실패가 다른 경기 중계를 멈추지 않게 한다.
				log.warn("[simulator] 경기 {} 이벤트 기록 실패: {}", gameId, error.getMessage());
			}
		}
	}

	/** 시작 시각이 지난 예정 경기를 시작한다. */
	private void startDueGames(LocalDateTime now) {
		gameRepository.findByProgress(GameProgress.NOT_STARTED, PageRequest.of(0, 50, Sort.by("startAt")))
				.getContent()
				.stream()
				.filter(game -> !game.getStartAt().isAfter(now))
				.forEach(game -> {
					gameLiveService.record(game.getId(),
							new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null));
					log.info("[simulator] 경기 {} 시작", game.getId());
				});
	}

	private void finish(LiveStateResponse state) {
		gameLiveService.record(state.gameId(),
				new LiveEventCreateRequest(GameEventType.GAME_FINISHED, null, null, state.homeScore(), state.awayScore()));
		log.info("[simulator] 경기 {} 종료 (예정 시각)", state.gameId());
	}

	/**
	 * 생성기가 돌리는 진행 중 경기. 예정 종료 시각이 있는 일정 배치 경기만 본다.
	 * 종료 시각이 없는 경기(관리자가 직접 만든 경기)는 관리자가 끝낼 때까지 자동으로 건드리지 않는다.
	 */
	private List<Long> liveGameIds() {
		LocalDateTime now = LocalDateTime.now(clock);
		return gameRepository.findByProgress(GameProgress.LIVE, Pageable.unpaged())
				.stream()
				.filter(game -> game.getPlannedEndAt() != null && !game.getStartAt().isAfter(now))
				.map(Game::getId)
				.toList();
	}

	void advance(LiveStateResponse state, RandomGenerator random) {
		if (state.progress() != GameProgress.LIVE) {
			return;
		}
		double roll = random.nextDouble();
		double score = SCORE_PROBABILITY * speed;
		double half = score + HALF_INNING_END_PROBABILITY * speed;
		double play = half + PLAY_PROBABILITY * speed;
		if (roll < score) {
			scoreOnce(state, random);
		} else if (roll < half) {
			endHalfInning(state);
		} else if (roll < play) {
			playOnce(state, random);
		}
	}

	/** 득점은 공격 중인 팀이 한다(초는 원정, 말은 홈). 타자는 그 팀 명단에서 고른다. */
	private void scoreOnce(LiveStateResponse state, RandomGenerator random) {
		Game game = gameRepository.findById(state.gameId()).orElseThrow();
		boolean homeBatting = state.half() == Half.BOTTOM;
		int home = state.homeScore() + (homeBatting ? 1 : 0);
		int away = state.awayScore() + (homeBatting ? 0 : 1);
		String battingTeam = homeBatting ? game.getHomeTeam() : game.getAwayTeam();
		List<Player> roster = playerRepository.findByTeamNameOrderByBackNumberAsc(battingTeam);
		Long playerId = roster.isEmpty() ? null : roster.get(random.nextInt(roster.size())).getId();
		String detail = SCORING_PLAYS.get(random.nextInt(SCORING_PLAYS.size())).name();
		gameLiveService.record(state.gameId(),
				new LiveEventCreateRequest(GameEventType.SCORE_CHANGED, null, null, home, away, playerId, detail));
	}

	/** 득점과 무관한 플레이 한 번. 타자(또는 주자)가 주인공이고, 투수가 맞서는 플레이면 투수도 함께 기록한다. */
	private void playOnce(LiveStateResponse state, RandomGenerator random) {
		Game game = gameRepository.findById(state.gameId()).orElseThrow();
		boolean homeBatting = state.half() == Half.BOTTOM;
		String battingTeam = homeBatting ? game.getHomeTeam() : game.getAwayTeam();
		String fieldingTeam = homeBatting ? game.getAwayTeam() : game.getHomeTeam();
		List<Player> batters = playerRepository.findByTeamNameOrderByBackNumberAsc(battingTeam);
		List<Player> pitchers = playerRepository.findByTeamNameOrderByBackNumberAsc(fieldingTeam).stream()
				.filter(player -> "투수".equals(player.getPosition()))
				.toList();
		if (batters.isEmpty()) {
			return;
		}
		PlayKind kind = PLAYS.get(random.nextInt(PLAYS.size()));
		Long batterId = batters.get(random.nextInt(batters.size())).getId();
		Long pitcherId = PITCHER_PLAYS.contains(kind) && !pitchers.isEmpty()
				? pitchers.get(random.nextInt(pitchers.size())).getId()
				: null;
		gameLiveService.record(state.gameId(),
				new LiveEventCreateRequest(GameEventType.PLAY, null, null, null, null, batterId, kind.name(), pitcherId));
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
