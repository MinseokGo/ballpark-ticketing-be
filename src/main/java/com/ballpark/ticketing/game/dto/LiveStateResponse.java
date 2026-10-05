package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.Half;

/** 현재 진행 상태 스냅샷. seq는 마지막으로 기록된 이벤트 번호다. */
public record LiveStateResponse(
		Long gameId,
		GameProgress progress,
		Integer inning,
		Half half,
		int homeScore,
		int awayScore,
		int seq) {

	public static LiveStateResponse from(Game game) {
		return new LiveStateResponse(
				game.getId(), game.getProgress(), game.getInning(), game.getHalf(),
				game.getHomeScore(), game.getAwayScore(), game.getEventSeq());
	}
}
