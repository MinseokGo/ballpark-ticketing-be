package com.ballpark.ticketing.game.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DailyMatchupPlannerTest {

	private final DailyMatchupPlanner planner = new DailyMatchupPlanner();

	@Test
	void pairsAllTenClubsIntoFiveGamesWithRealisticLengths() {
		List<Matchup> matchups = planner.plan(LocalDate.of(2026, 10, 6), RandomGenerator.of("L64X128MixRandom"));

		assertThat(matchups).hasSize(5);
		assertThat(matchups.stream().flatMap(m -> Stream.of(m.homeTeam(), m.awayTeam())).distinct())
				.containsExactlyInAnyOrderElementsOf(KboTeams.NAMES);
		for (Matchup matchup : matchups) {
			long minutes = Duration.between(matchup.startAt(), matchup.plannedEndAt()).toMinutes();
			assertThat(minutes).isBetween(120L, 420L);
			assertThat(minutes % 30).isZero();
			assertThat(matchup.startAt().toLocalTime()).isEqualTo(DailyMatchupPlanner.FIRST_PITCH);
		}
	}
}
