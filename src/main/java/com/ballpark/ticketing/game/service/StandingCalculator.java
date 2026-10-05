package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.game.dto.FinishedGameResult;
import com.ballpark.ticketing.game.dto.StandingResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 종료된 경기 결과로 팀 순위를 만든다. 저장소나 시각에 기대지 않는 순수 계산이라 단위 테스트로 규칙을 검증한다.
 * 정렬은 승률, 승수, 패수(적을수록 위), 팀 이름 순이다. 같은 승률의 팀이 한 자리에 몰려도 순서가 매번 같다.
 */
public final class StandingCalculator {

	private static final int WINS = 0;
	private static final int LOSSES = 1;
	private static final int DRAWS = 2;

	private StandingCalculator() {
	}

	/** 팀 목록에 있는 팀만 집계한다. 목록에 없는 팀의 경기는 무시한다. */
	public static List<StandingResponse> calculate(List<String> teams, List<FinishedGameResult> results) {
		Map<String, int[]> records = new HashMap<>();
		for (String team : teams) {
			records.put(team, new int[3]);
		}
		for (FinishedGameResult result : results) {
			int[] home = records.get(result.homeTeam());
			int[] away = records.get(result.awayTeam());
			if (home == null || away == null) {
				continue;
			}
			if (result.homeScore() == result.awayScore()) {
				home[DRAWS]++;
				away[DRAWS]++;
			} else if (result.homeScore() > result.awayScore()) {
				home[WINS]++;
				away[LOSSES]++;
			} else {
				away[WINS]++;
				home[LOSSES]++;
			}
		}

		List<String> ordered = new ArrayList<>(teams);
		ordered.sort(Comparator
				.comparingDouble((String team) -> winRate(records.get(team)[WINS], records.get(team)[LOSSES])).reversed()
				.thenComparing(Comparator.comparingInt((String team) -> records.get(team)[WINS]).reversed())
				.thenComparingInt(team -> records.get(team)[LOSSES])
				.thenComparing(Comparator.naturalOrder()));

		int[] leader = ordered.isEmpty() ? new int[3] : records.get(ordered.get(0));
		List<StandingResponse> standings = new ArrayList<>();
		for (int index = 0; index < ordered.size(); index++) {
			String team = ordered.get(index);
			int[] record = records.get(team);
			int wins = record[WINS];
			int losses = record[LOSSES];
			int draws = record[DRAWS];
			double gamesBehind = ((leader[WINS] - wins) + (losses - leader[LOSSES])) / 2.0;
			standings.add(new StandingResponse(index + 1, team, wins + losses + draws, wins, losses, draws,
					round3(winRate(wins, losses)), gamesBehind));
		}
		return standings;
	}

	private static double winRate(int wins, int losses) {
		return wins + losses == 0 ? 0.0 : (double) wins / (wins + losses);
	}

	private static double round3(double value) {
		return Math.round(value * 1000) / 1000.0;
	}
}
