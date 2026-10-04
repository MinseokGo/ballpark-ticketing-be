package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameStatus;
import java.time.LocalDateTime;

public record GameSummaryResponse(
		Long id, String homeTeam, String awayTeam, LocalDateTime startAt, GameStatus status) {

	public static GameSummaryResponse from(Game game) {
		return new GameSummaryResponse(
				game.getId(), game.getHomeTeam(), game.getAwayTeam(), game.getStartAt(), game.getStatus());
	}
}
