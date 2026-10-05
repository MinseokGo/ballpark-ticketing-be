package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.GameSeatStatus;
import java.util.List;

/** 좌석 판매 상태가 바뀐 한 자리. 실시간 좌석표에 쓴다. */
public record SeatStatusResponse(Long gameSeatId, GameSeatStatus status) {

	public static List<SeatStatusResponse> of(List<GameSeat> seats, GameSeatStatus status) {
		return seats.stream().map(seat -> new SeatStatusResponse(seat.getId(), status)).toList();
	}
}
