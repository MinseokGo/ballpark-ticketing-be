package com.ballpark.ticketing.game.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballpark.ticketing.game.dto.FinishedGameResult;
import com.ballpark.ticketing.game.dto.StandingResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandingCalculatorTest {

	private static final List<String> TEAMS = List.of("Seoul Comets", "Busan Gulls", "Daegu Owls");

	@Test
	void ranksByWinRateAndExcludesDrawsFromIt() {
		// 서울 1승 1무(승률 1.000), 부산 1승 1패(.500), 대구 1패 1무(.000)
		List<FinishedGameResult> results = List.of(
				new FinishedGameResult("Seoul Comets", "Busan Gulls", 5, 2),
				new FinishedGameResult("Seoul Comets", "Daegu Owls", 3, 3),
				new FinishedGameResult("Busan Gulls", "Daegu Owls", 4, 1));

		List<StandingResponse> standings = StandingCalculator.calculate(TEAMS, results);

		assertThat(standings).extracting(StandingResponse::teamName)
				.containsExactly("Seoul Comets", "Busan Gulls", "Daegu Owls");
		StandingResponse seoul = standings.get(0);
		assertThat(seoul.rank()).isEqualTo(1);
		assertThat(seoul.wins()).isEqualTo(1);
		assertThat(seoul.losses()).isZero();
		assertThat(seoul.draws()).isEqualTo(1);
		assertThat(seoul.games()).isEqualTo(2);
		assertThat(seoul.winRate()).isEqualTo(1.0);
	}

	@Test
	void computesGamesBehindFromTheLeader() {
		// 서울 5승 0패(1위), 대구 1승 1패, 부산 0승 5패
		List<FinishedGameResult> results = List.of(
				new FinishedGameResult("Seoul Comets", "Busan Gulls", 5, 2),
				new FinishedGameResult("Seoul Comets", "Busan Gulls", 4, 1),
				new FinishedGameResult("Busan Gulls", "Seoul Comets", 1, 3),
				new FinishedGameResult("Seoul Comets", "Busan Gulls", 3, 2),
				new FinishedGameResult("Seoul Comets", "Daegu Owls", 1, 0),
				new FinishedGameResult("Daegu Owls", "Busan Gulls", 2, 0));

		List<StandingResponse> standings = StandingCalculator.calculate(TEAMS, results);

		assertThat(standings).extracting(StandingResponse::teamName)
				.containsExactly("Seoul Comets", "Daegu Owls", "Busan Gulls");
		assertThat(standings.get(0).gamesBehind()).isZero();
		// 대구: ((5-1) + (1-0)) / 2 = 2.5
		assertThat(standings.get(1).gamesBehind()).isEqualTo(2.5);
		// 부산: ((5-0) + (5-0)) / 2 = 5.0
		assertThat(standings.get(2).gamesBehind()).isEqualTo(5.0);
	}

	@Test
	void teamsWithoutGamesStayInTheTableAndUnknownTeamsAreIgnored() {
		List<FinishedGameResult> results = List.of(
				new FinishedGameResult("Seoul Comets", "Unknown Team", 3, 0));

		List<StandingResponse> standings = StandingCalculator.calculate(TEAMS, results);

		assertThat(standings).hasSize(3);
		assertThat(standings).allSatisfy(row -> assertThat(row.games()).isZero());
		assertThat(standings).allSatisfy(row -> assertThat(row.winRate()).isZero());
		assertThat(standings.get(0).teamName()).isEqualTo("Busan Gulls");
	}
}
