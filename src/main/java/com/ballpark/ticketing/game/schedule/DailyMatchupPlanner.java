package com.ballpark.ticketing.game.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * 하루치 대진을 짠다. 10개 구단을 무작위로 섞어 5경기를 만들고, 경기 시간은 2시간에서 7시간 사이(30분 단위)로 정한다.
 * 시간을 정하는 규칙만 가진다. 경기를 저장하는 일은 하지 않는다.
 */
@Component
public class DailyMatchupPlanner {

	static final LocalTime FIRST_PITCH = LocalTime.of(18, 30);
	static final int MIN_DURATION_MINUTES = 120;
	static final int MAX_DURATION_MINUTES = 420;
	static final int DURATION_STEP_MINUTES = 30;

	public List<Matchup> plan(LocalDate day, RandomGenerator random) {
		List<String> teams = new ArrayList<>(KboTeams.NAMES);
		Collections.shuffle(teams, java.util.Random.from(random));
		LocalDateTime startAt = day.atTime(FIRST_PITCH);
		List<Matchup> matchups = new ArrayList<>();
		for (int index = 0; index + 1 < teams.size(); index += 2) {
			int steps = (MAX_DURATION_MINUTES - MIN_DURATION_MINUTES) / DURATION_STEP_MINUTES;
			int duration = MIN_DURATION_MINUTES + random.nextInt(steps + 1) * DURATION_STEP_MINUTES;
			matchups.add(new Matchup(teams.get(index), teams.get(index + 1), startAt,
					startAt.plusMinutes(duration)));
		}
		return matchups;
	}
}
