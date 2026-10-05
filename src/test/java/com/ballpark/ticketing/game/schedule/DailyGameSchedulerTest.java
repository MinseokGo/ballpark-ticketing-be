package com.ballpark.ticketing.game.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import com.ballpark.ticketing.TestcontainersConfiguration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class DailyGameSchedulerTest {

	@Autowired
	private DailyGameScheduler scheduler;

	@Test
	void createsFiveGamesForADayOnlyOnce() {
		LocalDate day = LocalDate.of(2031, 4, 2);

		assertThat(scheduler.createFor(day)).isEqualTo(5);
		assertThat(scheduler.createFor(day)).isZero();
	}
}
