package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.GameStatus;
import com.ballpark.ticketing.game.Winner;
import java.time.LocalDateTime;

/** 목록 응답. 진행 상태와 점수를 같이 줘서 홈·일정 화면이 추가 호출 없이 라이브·결과를 그릴 수 있게 한다. */
public record GameSummaryResponse(
		Long id,
		String homeTeam,
		String awayTeam,
		LocalDateTime startAt,
		GameStatus status,
		GameProgress progress,
		int homeScore,
		int awayScore,
		Winner winner) {

	public static GameSummaryResponse from(Game game) {
		return new GameSummaryResponse(
				game.getId(), game.getHomeTeam(), game.getAwayTeam(), game.getStartAt(), game.getStatus(),
				game.getProgress(), game.getHomeScore(), game.getAwayScore(), game.winner());
	}
}
