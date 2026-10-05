package com.ballpark.ticketing.game.schedule;

import java.util.List;

/**
 * KBO 10개 구단 이름. 데모 데이터에서만 쓴다(CLAUDE.md 테스트 절의 예외: 실제 구단 이름, 일정은 임의 값).
 * 선수 명단과 일정 배치가 같은 목록을 쓰도록 한 곳에 둔다.
 */
public final class KboTeams {

	public static final List<String> NAMES = List.of(
			"두산 베어스", "LG 트윈스", "KIA 타이거즈", "삼성 라이온즈", "SSG 랜더스",
			"롯데 자이언츠", "한화 이글스", "NC 다이노스", "KT 위즈", "키움 히어로즈");

	private KboTeams() {
	}
}
