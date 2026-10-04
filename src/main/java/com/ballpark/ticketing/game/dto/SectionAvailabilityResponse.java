package com.ballpark.ticketing.game.dto;

/** 경기 하나에서 구역별 좌석 현황. GameSeatRepository의 집계 쿼리 결과를 바로 담는다. */
public record SectionAvailabilityResponse(
		Long sectionId,
		String name,
		String grade,
		long price,
		long totalSeats,
		long availableSeats,
		long heldSeats,
		long soldSeats) {
}
