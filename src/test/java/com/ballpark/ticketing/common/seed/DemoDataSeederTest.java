package com.ballpark.ticketing.common.seed;

import static org.assertj.core.api.Assertions.assertThat;
import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.repository.GameSeatRepository;
import com.ballpark.ticketing.game.repository.SeatRepository;
import com.ballpark.ticketing.game.repository.SectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"test", "seed"})
class DemoDataSeederTest {

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private GameSeatRepository gameSeatRepository;

	@Test
	void seedsThirtySectionsTwentyTwoThousandSeatsAndFiveGames() {
		assertThat(sectionRepository.count()).isEqualTo(30);
		assertThat(seatRepository.count()).isEqualTo(22_536);
		assertThat(gameRepository.count()).isEqualTo(5);
		assertThat(gameSeatRepository.count()).isEqualTo(22_536L * 5);
	}
}
