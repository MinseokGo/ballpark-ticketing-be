package com.ballpark.ticketing.game.dto;

/**
 * 팀 순위 한 줄. 승률은 무승부를 뺀 승/(승+패)이고, 게임차는 1위와의 차이다(1위는 0).
 * 승률은 소수 셋째 자리까지 반올림한다(예: 0.571).
 */
public record StandingResponse(
		int rank,
		String teamName,
		int games,
		int wins,
		int losses,
		int draws,
		double winRate,
		double gamesBehind) {
}
