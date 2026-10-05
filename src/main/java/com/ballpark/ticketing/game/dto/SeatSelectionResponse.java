package com.ballpark.ticketing.game.dto;

/** 다른 사용자가 고르는 중인 좌석 한 자리. 예매가 아니라 표시용이다. */
public record SeatSelectionResponse(Long gameSeatId, Long userId) {
}
