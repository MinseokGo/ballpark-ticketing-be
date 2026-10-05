package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.Half;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 관리자가 보내는 진행 이벤트. 타입마다 필요한 값이 다르다(예: INNING_CHANGED는 inning·half).
 * 필수 값 검사는 서비스가 하고, 상태 전이 규칙은 Game이 검사한다.
 */
public record LiveEventCreateRequest(
		@NotNull GameEventType type,
		@Min(1) Integer inning,
		Half half,
		@Min(0) Integer homeScore,
		@Min(0) Integer awayScore,
		Long playerId,
		@Size(max = 20) String detail,
		Long secondaryPlayerId) {

	public LiveEventCreateRequest(GameEventType type, Integer inning, Half half, Integer homeScore, Integer awayScore) {
		this(type, inning, half, homeScore, awayScore, null, null, null);
	}

	public LiveEventCreateRequest(GameEventType type, Integer inning, Half half, Integer homeScore, Integer awayScore,
			Long playerId, String detail) {
		this(type, inning, half, homeScore, awayScore, playerId, detail, null);
	}
}
