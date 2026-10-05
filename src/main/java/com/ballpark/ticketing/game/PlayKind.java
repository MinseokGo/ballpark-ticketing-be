package com.ballpark.ticketing.game;

/** 한 번의 플레이 종류. 득점 이벤트의 기록 종류와 PLAY 이벤트의 종류로 쓴다. */
public enum PlayKind {
	HIT,
	DOUBLE,
	TRIPLE,
	HOME_RUN,
	WALK,
	HIT_BY_PITCH,
	STRIKEOUT,
	GROUND_OUT,
	FLY_OUT,
	DOUBLE_PLAY,
	STOLEN_BASE,
	CAUGHT_STEALING,
	SACRIFICE_FLY,
	SACRIFICE_BUNT,
	ERROR,
	WILD_PITCH,
	PASSED_BALL
}
