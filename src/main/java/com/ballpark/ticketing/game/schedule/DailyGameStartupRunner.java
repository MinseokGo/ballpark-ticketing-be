package com.ballpark.ticketing.game.schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 서버가 뜰 때 경기를 보충한다. 자정 배치를 놓쳤을 때를 위한 것이다. 테스트 프로필에서는 돌지 않는다.
 * 오늘 첫 경기 시각(18:30)이 아직 안 지났으면 오늘 경기를, 지났으면 내일 경기를 만든다.
 * 이미 지난 시각으로 경기를 만들면 중계가 한가운데서 시작되므로 그렇게 하지 않는다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DailyGameStartupRunner implements ApplicationRunner {

	private final DailyGameScheduler scheduler;
	private final Clock clock;

	@Override
	public void run(ApplicationArguments args) {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDate day = now.toLocalTime().isBefore(DailyMatchupPlanner.FIRST_PITCH)
				? now.toLocalDate()
				: now.toLocalDate().plusDays(1);
		int created = scheduler.createFor(day);
		log.info("[schedule] 기동 시 {} 경기 보충: {}개", day, created);
	}
}
