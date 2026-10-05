package com.ballpark.ticketing.game.simulator;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.PlayKind;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.dto.LiveStateResponse;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.service.GameLiveService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 데모용 중계 생성기. 시작 시각이 된 배치 경기를 시작하고, 진행 중인 경기에 이벤트를 랜덤으로 쌓는다.
 * 모든 이벤트는 관리자 API와 같은 경로(GameLiveService)로 들어가서, DB 기록과 SSE 전송이 실제 입력과 같다.
 * 무엇이 일어날지는 EventRoll, 선수는 RosterPicker, 이닝 규칙은 InningRules가 정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveGameSimulator {

	// 득점 외 일반 플레이. 삼진·볼넷·병살·도루 등 기록 화면에서 보여준다.
	private static final List<PlayKind> PLAYS = List.of(
			PlayKind.STRIKEOUT, PlayKind.STRIKEOUT, PlayKind.WALK, PlayKind.GROUND_OUT, PlayKind.FLY_OUT,
			PlayKind.DOUBLE_PLAY, PlayKind.STOLEN_BASE, PlayKind.CAUGHT_STEALING, PlayKind.HIT_BY_PITCH,
			PlayKind.ERROR, PlayKind.HIT, PlayKind.DOUBLE);
	private static final List<PlayKind> SCORING_PLAYS = List.of(
			PlayKind.HIT, PlayKind.DOUBLE, PlayKind.HOME_RUN, PlayKind.SACRIFICE_FLY, PlayKind.WILD_PITCH);
	// 투수가 함께 기록되는 플레이
	private static final Set<PlayKind> PITCHER_PLAYS = EnumSet.of(
			PlayKind.STRIKEOUT, PlayKind.WALK, PlayKind.HIT_BY_PITCH, PlayKind.GROUND_OUT, PlayKind.FLY_OUT,
			PlayKind.DOUBLE_PLAY);

	private final GameRepository gameRepository;
	private final GameLiveService gameLiveService;
	private final RosterPicker rosterPicker;
	private final LiveSimulatorProperties properties;
	private final Clock clock;
	private final RandomGenerator random = RandomGenerator.getDefault();

	@Scheduled(fixedDelay = 2_000)
	public void scheduledTick() {
		if (properties.enabled()) {
			tick(random);
		}
	}

	/** 한 번 돈다: 시작 시각이 된 경기를 시작하고, 진행 중 경기마다 이벤트를 한 번 줄 수 있다. 예정 종료가 지나면 끝낸다. */
	public void tick(RandomGenerator random) {
		LocalDateTime now = LocalDateTime.now(clock);
		startDueGames(now);
		for (Long gameId : liveGameIds(now)) {
			try {
				Game game = gameRepository.findById(gameId).orElseThrow();
				LiveStateResponse state = gameLiveService.snapshot(gameId);
				if (game.isPastPlannedEnd(now)) {
					gameLiveService.record(gameId, InningRules.finish(state));
					log.info("[simulator] 경기 {} 종료 (예정 시각)", gameId);
				} else {
					advance(game, state, random);
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

	/** 생성기가 돌리는 진행 중 경기. 예정 종료 시각이 있는 배치 경기만 본다. */
	private List<Long> liveGameIds(LocalDateTime now) {
		return gameRepository.findByProgress(GameProgress.LIVE, Pageable.unpaged())
				.stream()
				.filter(game -> game.getPlannedEndAt() != null && !game.getStartAt().isAfter(now))
				.map(Game::getId)
				.toList();
	}

	void advance(Game game, LiveStateResponse state, RandomGenerator random) {
		if (state.progress() != GameProgress.LIVE) {
			return;
		}
		switch (new EventRoll(properties.speed()).pick(random)) {
			case SCORE -> scoreOnce(game, state, random);
			case HALF_INNING_END -> gameLiveService.record(state.gameId(), InningRules.endOfHalfInning(state));
			case PLAY -> playOnce(game, state, random);
			case NONE -> {
			}
		}
	}

	/** 득점은 공격 중인 팀이 한다. 타자는 그 팀 명단에서 고른다. */
	private void scoreOnce(Game game, LiveStateResponse state, RandomGenerator random) {
		boolean homeBatting = InningRules.isHomeBatting(state.half());
		int home = state.homeScore() + (homeBatting ? 1 : 0);
		int away = state.awayScore() + (homeBatting ? 0 : 1);
		Optional<Long> scorer = rosterPicker.anyPlayer(battingTeam(game, state), random);
		String detail = SCORING_PLAYS.get(random.nextInt(SCORING_PLAYS.size())).name();
		gameLiveService.record(state.gameId(),
				new LiveEventCreateRequest(GameEventType.SCORE_CHANGED, null, null, home, away, scorer.orElse(null), detail));
	}

	/** 득점과 무관한 플레이 한 번. 타자(또는 주자)가 주인공이고, 투수가 맞서는 플레이면 투수도 함께 기록한다. */
	private void playOnce(Game game, LiveStateResponse state, RandomGenerator random) {
		Optional<Long> batter = rosterPicker.anyPlayer(battingTeam(game, state), random);
		if (batter.isEmpty()) {
			return;
		}
		PlayKind kind = PLAYS.get(random.nextInt(PLAYS.size()));
		Long pitcher = PITCHER_PLAYS.contains(kind)
				? rosterPicker.pitcher(fieldingTeam(game, state), random).orElse(null)
				: null;
		gameLiveService.record(state.gameId(),
				new LiveEventCreateRequest(GameEventType.PLAY, null, null, null, null, batter.get(), kind.name(), pitcher));
	}

	private static String battingTeam(Game game, LiveStateResponse state) {
		return InningRules.isHomeBatting(state.half()) ? game.getHomeTeam() : game.getAwayTeam();
	}

	private static String fieldingTeam(Game game, LiveStateResponse state) {
		return InningRules.isHomeBatting(state.half()) ? game.getAwayTeam() : game.getHomeTeam();
	}
}
