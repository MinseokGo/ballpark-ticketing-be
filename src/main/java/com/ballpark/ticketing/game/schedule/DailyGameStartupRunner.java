package com.ballpark.ticketing.game.schedule;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 서버가 뜰 때 오늘 경기가 없으면 만든다. 자정 배치를 놓쳤을 때를 위한 것이다. 테스트 프로필에서는 돌지 않는다. */
@Component
@Profile("!test")
public class DailyGameStartupRunner implements ApplicationRunner {

	private final DailyGameScheduler scheduler;
	private final Clock clock;

	public DailyGameStartupRunner(DailyGameScheduler scheduler, Clock clock) {
		this.scheduler = scheduler;
		this.clock = clock;
	}

	@Override
	public void run(ApplicationArguments args) {
		scheduler.createFor(LocalDate.now(clock));
	}
}
