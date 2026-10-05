package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.GameStatus;
import com.ballpark.ticketing.game.Winner;
import java.time.LocalDateTime;

public record GameResponse(
		Long id,
		String homeTeam,
		String awayTeam,
		LocalDateTime startAt,
		LocalDateTime ticketOpenAt,
		GameStatus status,
		int gameSeatCount,
		GameProgress progress,
		int homeScore,
		int awayScore,
		Winner winner) {

	public static GameResponse of(Game game, int gameSeatCount) {
		return new GameResponse(
				game.getId(), game.getHomeTeam(), game.getAwayTeam(),
				game.getStartAt(), game.getTicketOpenAt(), game.getStatus(), gameSeatCount,
				game.getProgress(), game.getHomeScore(), game.getAwayScore(), game.winner());
	}
}
