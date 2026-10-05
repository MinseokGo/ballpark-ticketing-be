package com.ballpark.ticketing.game.schedule;

import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.service.GameService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 하루 경기를 만드는 배치. 매일 자정(한국 시간)에 그날 경기를 만들고, 예매를 바로 연다.
 * 같은 날짜의 경기가 이미 있으면 아무것도 하지 않는다(재시작해도 중복 생성이 없다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyGameScheduler {

	private final GameService gameService;
	private final GameRepository gameRepository;
	private final DailyMatchupPlanner planner;
	private final Clock clock;
	private final RandomGenerator random = RandomGenerator.getDefault();

	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void createToday() {
		createFor(LocalDate.now(clock));
	}

	/** 주어진 날짜의 경기를 만든다. 이미 있으면 0을 돌려준다. */
	@Transactional
	public int createFor(LocalDate day) {
		var from = day.atStartOfDay();
		if (gameRepository.countByStartAtBetween(from, day.plusDays(1).atStartOfDay()) > 0) {
			return 0;
		}
		var matchups = planner.plan(day, random);
		for (Matchup matchup : matchups) {
			GameResponse game = gameService.create(new GameCreateRequest(
					matchup.homeTeam(), matchup.awayTeam(), matchup.startAt(), matchup.startAt().minusDays(1)));
			gameService.openTicketing(game.id());
			gameRepository.findById(game.id()).ifPresent(entity -> entity.planEnd(matchup.plannedEndAt()));
		}
		log.info("[schedule] {} 경기 {}개 생성", day, matchups.size());
		return matchups.size();
	}
}
