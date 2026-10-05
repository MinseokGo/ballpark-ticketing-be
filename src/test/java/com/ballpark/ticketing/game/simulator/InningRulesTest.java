package com.ballpark.ticketing.game.simulator;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.Half;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.dto.LiveStateResponse;
import org.junit.jupiter.api.Test;

class InningRulesTest {

	@Test
	void topOfAnInningGoesToBottomOfTheSameInning() {
		LiveEventCreateRequest next = InningRules.endOfHalfInning(state(3, Half.TOP, 2, 1));

		assertThat(next.type()).isEqualTo(GameEventType.INNING_CHANGED);
		assertThat(next.inning()).isEqualTo(3);
		assertThat(next.half()).isEqualTo(Half.BOTTOM);
	}

	@Test
	void bottomOfAnInningGoesToTopOfTheNextInning() {
		LiveEventCreateRequest next = InningRules.endOfHalfInning(state(4, Half.BOTTOM, 2, 1));

		assertThat(next.type()).isEqualTo(GameEventType.INNING_CHANGED);
		assertThat(next.inning()).isEqualTo(5);
		assertThat(next.half()).isEqualTo(Half.TOP);
	}

	@Test
	void endsAfterNineInningsWhenTheScoreIsNotTied() {
		LiveEventCreateRequest end = InningRules.endOfHalfInning(state(9, Half.BOTTOM, 4, 2));

		assertThat(end.type()).isEqualTo(GameEventType.GAME_FINISHED);
		assertThat(end.homeScore()).isEqualTo(4);
		assertThat(end.awayScore()).isEqualTo(2);
	}

	@Test
	void keepsGoingWhileTiedUntilTheForcedEnd() {
		assertThat(InningRules.endOfHalfInning(state(9, Half.BOTTOM, 2, 2)).type())
				.isEqualTo(GameEventType.INNING_CHANGED);
		assertThat(InningRules.endOfHalfInning(state(InningRules.FORCED_END_INNING, Half.BOTTOM, 2, 2)).type())
				.isEqualTo(GameEventType.GAME_FINISHED);
	}

	private static LiveStateResponse state(int inning, Half half, int homeScore, int awayScore) {
		return new LiveStateResponse(1L, GameProgress.LIVE, inning, half, homeScore, awayScore, 0);
	}
}
