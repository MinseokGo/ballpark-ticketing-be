package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.GameEvent;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.Half;

public record LiveEventResponse(
		Long gameId,
		int seq,
		GameEventType type,
		Integer inning,
		Half half,
		int homeScore,
		int awayScore) {

	public static LiveEventResponse from(GameEvent event) {
		return new LiveEventResponse(
				event.getGame().getId(), event.getSeq(), event.getType(), event.getInning(), event.getHalf(),
				event.getHomeScore(), event.getAwayScore());
	}

	/** 스트림이 끝나는 이벤트인지. 종료되거나 취소된 경기는 여기서 연결을 닫는다. */
	public boolean isTerminal() {
		return type == GameEventType.GAME_FINISHED || type == GameEventType.GAME_CANCELLED;
	}
}
