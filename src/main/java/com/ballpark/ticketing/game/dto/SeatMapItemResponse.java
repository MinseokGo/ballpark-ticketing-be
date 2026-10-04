package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.GameSeatStatus;

/** 경기 하나의 좌석맵 한 칸. GameSeatRepository의 조인 쿼리 결과를 바로 담는다(N+1 없이 한 번에 조회). */
public record SeatMapItemResponse(
		Long gameSeatId,
		Long seatId,
		int rowNo,
		int seatNo,
		Long sectionId,
		String sectionName,
		String grade,
		GameSeatStatus status) {
}
