package com.ballpark.ticketing.game.simulator;

import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.Half;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.dto.LiveStateResponse;

/** 이닝 규칙. 정규 9이닝 뒤 점수가 다르면 끝내고, 동점이면 12회에서 강제로 끝낸다. */
final class InningRules {

	static final int REGULAR_INNINGS = 9;
	static final int FORCED_END_INNING = 12;

	private InningRules() {
	}

	/** 공격 중인 팀이 어느 팀인지. 초는 원정, 말은 홈이 공격한다. */
	static boolean isHomeBatting(Half half) {
		return half == Half.BOTTOM;
	}

	/** 공격이 끝났을 때 보낼 이벤트. 경기를 끝내거나 다음 이닝(말 → 다음 회 초)으로 넘긴다. */
	static LiveEventCreateRequest endOfHalfInning(LiveStateResponse state) {
		boolean bottom = state.half() == Half.BOTTOM;
		int inning = state.inning();
		if (bottom && inning >= REGULAR_INNINGS && state.homeScore() != state.awayScore()) {
			return finish(state);
		}
		if (bottom && inning >= FORCED_END_INNING) {
			return finish(state);
		}
		if (bottom) {
			return new LiveEventCreateRequest(GameEventType.INNING_CHANGED, inning + 1, Half.TOP, null, null);
		}
		return new LiveEventCreateRequest(GameEventType.INNING_CHANGED, inning, Half.BOTTOM, null, null);
	}

	static LiveEventCreateRequest finish(LiveStateResponse state) {
		return new LiveEventCreateRequest(GameEventType.GAME_FINISHED, null, null, state.homeScore(), state.awayScore());
	}
}
