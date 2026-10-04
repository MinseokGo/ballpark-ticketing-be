package com.ballpark.ticketing.game;

/**
 * 좌석 일괄 생성 시 좌석 위치(행·열)만 담는 값. JDBC 배치 삽입에서 엔티티 생성 없이 쓴다.
 */
public record SeatPosition(int rowNo, int seatNo) {
}
