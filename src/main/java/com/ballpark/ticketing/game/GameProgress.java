package com.ballpark.ticketing.game;

/** 경기 진행 상태. 예매 상태(GameStatus)와 따로 둔다: 예매 마감과 경기 종료는 다른 개념이다. */
public enum GameProgress {
	NOT_STARTED,
	LIVE,
	FINISHED,
	CANCELLED
}
