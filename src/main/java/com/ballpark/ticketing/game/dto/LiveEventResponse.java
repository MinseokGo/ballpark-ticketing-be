package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.GameEvent;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.Half;
import com.ballpark.ticketing.game.Player;
import java.time.LocalDateTime;

public record LiveEventResponse(
		Long gameId,
		int seq,
		GameEventType type,
		Integer inning,
		Half half,
		int homeScore,
		int awayScore,
		Long playerId,
		String playerName,
		String teamName,
		String detail,
		Long secondaryPlayerId,
		String secondaryPlayerName,
		LocalDateTime createdAt) {

	public static LiveEventResponse from(GameEvent event) {
		Player player = event.getPlayer();
		Player secondary = event.getSecondaryPlayer();
		return new LiveEventResponse(
				event.getGame().getId(), event.getSeq(), event.getType(), event.getInning(), event.getHalf(),
				event.getHomeScore(), event.getAwayScore(),
				player == null ? null : player.getId(),
				player == null ? null : player.getName(),
				player == null ? null : player.getTeamName(),
				event.getDetail(),
				secondary == null ? null : secondary.getId(),
				secondary == null ? null : secondary.getName(),
				event.getCreatedAt());
	}

	/** 스트림이 끝나는 이벤트인지. 종료되거나 취소된 경기는 여기서 연결을 닫는다. */
	public boolean isTerminal() {
		return type == GameEventType.GAME_FINISHED || type == GameEventType.GAME_CANCELLED;
	}
}
