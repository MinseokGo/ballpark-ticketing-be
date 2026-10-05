package com.ballpark.ticketing.game.simulator;

import java.util.random.RandomGenerator;

/**
 * 틱 한 번에 무슨 일이 일어날지 고른다. 확률은 2초마다 한 번 도는 기준으로 맞춘 실제 페이스다.
 * 득점은 5~8분에 한 번, 이닝 교체는 10분 안팎, 플레이는 1~2분에 한 번꼴이다. speed로 배속한다.
 */
record EventRoll(double speed) {

	private static final double SCORE_PROBABILITY = 0.005;
	private static final double HALF_INNING_END_PROBABILITY = 0.0033;
	private static final double PLAY_PROBABILITY = 0.03;

	enum Outcome {
		NONE, SCORE, HALF_INNING_END, PLAY
	}

	Outcome pick(RandomGenerator random) {
		double roll = random.nextDouble();
		double score = SCORE_PROBABILITY * speed;
		double half = score + HALF_INNING_END_PROBABILITY * speed;
		double play = half + PLAY_PROBABILITY * speed;
		if (roll < score) {
			return Outcome.SCORE;
		}
		if (roll < half) {
			return Outcome.HALF_INNING_END;
		}
		if (roll < play) {
			return Outcome.PLAY;
		}
		return Outcome.NONE;
	}
}
