package com.ballpark.ticketing.game.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.game.Section;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.SeatBulkCreateRequest;
import com.ballpark.ticketing.game.repository.GameSeatRepository;
import com.ballpark.ticketing.game.repository.SectionRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class GameServiceTest {

	@Autowired
	private GameService gameService;

	@Autowired
	private SeatService seatService;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private GameSeatRepository gameSeatRepository;

	@Test
	void creatingAGameWithNoSeatsYieldsNoGameSeats() {
		GameResponse response = gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0)));

		assertThat(response.gameSeatCount()).isZero();
	}

	@Test
	void creatingAGameGeneratesOneGameSeatPerExistingSeat() {
		Section section = sectionRepository.save(new Section("Infield 401", "R", 30_000));
		seatService.createBulk(section.getId(), new SeatBulkCreateRequest(5, 10));

		GameResponse response = gameService.create(new GameCreateRequest(
				"Daegu Owls", "Seoul Comets",
				LocalDateTime.of(2026, 11, 2, 18, 30), LocalDateTime.of(2026, 10, 26, 11, 0)));

		assertThat(response.gameSeatCount()).isEqualTo(50);
		assertThat(gameSeatRepository.count()).isEqualTo(50);
	}
}
