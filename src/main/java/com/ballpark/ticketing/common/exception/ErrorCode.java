package com.ballpark.ticketing.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

	// 공통
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 올바르지 않습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-002", "요청을 처리할 수 없습니다."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-003", "요청한 리소스를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-004", "지원하지 않는 HTTP 메서드입니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON-005", "지원하지 않는 Content-Type입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-999", "서버 내부 오류가 발생했습니다."),

	// 구역·좌석
	INVALID_SECTION_PRICE(HttpStatus.BAD_REQUEST, "SEAT-001", "구역 가격은 0 이상이어야 합니다."),
	SEAT_NOT_AVAILABLE(HttpStatus.CONFLICT, "SEAT-002", "선점할 수 없는 좌석입니다."),
	SEAT_NOT_HELD(HttpStatus.CONFLICT, "SEAT-003", "선점되지 않은 좌석은 판매할 수 없습니다."),
	SEAT_ALREADY_AVAILABLE(HttpStatus.CONFLICT, "SEAT-004", "이미 판매 가능한 좌석입니다."),
	SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SEAT-005", "존재하지 않는 구역입니다."),
	SECTION_NAME_DUPLICATE(HttpStatus.CONFLICT, "SEAT-006", "이미 존재하는 구역 이름입니다."),

	// 경기
	INVALID_GAME_SCHEDULE(HttpStatus.BAD_REQUEST, "GAME-001", "예매 오픈 시각은 경기 시작 시각보다 이전이어야 합니다."),
	GAME_NOT_SCHEDULED(HttpStatus.CONFLICT, "GAME-002", "예정 상태의 경기만 예매를 오픈할 수 있습니다."),
	GAME_NOT_OPEN(HttpStatus.CONFLICT, "GAME-003", "예매가 오픈된 경기가 아닙니다."),

	// 예약
	RESERVATION_SEATS_EMPTY(HttpStatus.BAD_REQUEST, "RESERVATION-001", "예약할 좌석을 1개 이상 선택해야 합니다."),
	RESERVATION_NOT_PENDING(HttpStatus.CONFLICT, "RESERVATION-002", "대기 상태의 예약만 확정할 수 있습니다."),
	RESERVATION_ALREADY_CANCELLED(HttpStatus.CONFLICT, "RESERVATION-003", "이미 취소된 예약입니다."),

	// 결제
	PAYMENT_NOT_PENDING(HttpStatus.CONFLICT, "PAYMENT-001", "대기 상태의 결제만 처리할 수 있습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
