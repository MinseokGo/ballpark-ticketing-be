package com.ballpark.ticketing.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/**
 * 구역을 행×열 격자로 보고 좌석을 일괄 생성한다. rowNo는 1..rowCount, seatNo는 1..seatsPerRow로 채워진다.
 */
public record SeatBulkCreateRequest(
		@Positive @Max(1000) int rowCount,
		@Positive @Max(1000) int seatsPerRow) {
}
