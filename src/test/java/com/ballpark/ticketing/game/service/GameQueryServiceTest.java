package com.ballpark.ticketing.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.common.dto.PageResponse;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.Section;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.GameSummaryResponse;
import com.ballpark.ticketing.game.dto.SeatBulkCreateRequest;
import com.ballpark.ticketing.game.dto.SeatMapItemResponse;
import com.ballpark.ticketing.game.dto.SectionAvailabilityResponse;
import com.ballpark.ticketing.game.repository.GameSeatRepository;
import com.ballpark.ticketing.game.repository.SectionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class GameQueryServiceTest {

	@Autowired
	private GameService gameService;

	@Autowired
	private GameQueryService gameQueryService;

	@Autowired
	private SeatService seatService;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private GameSeatRepository gameSeatRepository;

	private GameResponse createGame(String homeTeam, String awayTeam) {
		return gameService.create(new GameCreateRequest(
				homeTeam, awayTeam,
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0)));
	}

	@Test
	void listsGamesPagedByStartAt() {
		createGame("Seoul Comets", "Busan Gulls");
		createGame("Daegu Owls", "Seoul Comets");
		createGame("Busan Gulls", "Daegu Owls");

		PageResponse<GameSummaryResponse> firstPage = gameQueryService.listGames(PageRequest.of(0, 2));

		assertThat(firstPage.content()).hasSize(2);
		assertThat(firstPage.totalElements()).isEqualTo(3);
		assertThat(firstPage.totalPages()).isEqualTo(2);
	}

	@Test
	void getGameReturnsDetailWithGameSeatCount() {
		Section section = sectionRepository.save(new Section("Infield 501", "R", 30_000));
		seatService.createBulk(section.getId(), new SeatBulkCreateRequest(2, 5));
		GameResponse created = createGame("Seoul Comets", "Busan Gulls");

		GameResponse found = gameQueryService.getGame(created.id());

		assertThat(found.homeTeam()).isEqualTo("Seoul Comets");
		assertThat(found.gameSeatCount()).isEqualTo(10);
	}

	@Test
	void getGameWithUnknownIdThrows() {
		assertThatThrownBy(() -> gameQueryService.getGame(999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_NOT_FOUND);
	}

	@Test
	void sectionAvailabilityAggregatesStatusCountsPerSection() {
		Section sectionA = sectionRepository.save(new Section("Infield A", "R", 30_000));
		Section sectionB = sectionRepository.save(new Section("Outfield B", "B", 10_000));
		seatService.createBulk(sectionA.getId(), new SeatBulkCreateRequest(1, 3));
		seatService.createBulk(sectionB.getId(), new SeatBulkCreateRequest(1, 2));
		GameResponse game = createGame("Seoul Comets", "Busan Gulls");

		List<GameSeat> sectionAGameSeats = gameSeatRepository.findAll().stream()
				.filter(gameSeat -> gameSeat.getSeat().getSection().getId().equals(sectionA.getId()))
				.toList();
		sectionAGameSeats.get(0).hold();
		sectionAGameSeats.get(1).hold();
		sectionAGameSeats.get(1).sell();

		List<SectionAvailabilityResponse> availability = gameQueryService.getSectionAvailability(game.id());

		SectionAvailabilityResponse sectionAStats = availability.stream()
				.filter(stats -> stats.sectionId().equals(sectionA.getId()))
				.findFirst()
				.orElseThrow();
		assertThat(sectionAStats.totalSeats()).isEqualTo(3);
		assertThat(sectionAStats.availableSeats()).isEqualTo(1);
		assertThat(sectionAStats.heldSeats()).isEqualTo(1);
		assertThat(sectionAStats.soldSeats()).isEqualTo(1);

		SectionAvailabilityResponse sectionBStats = availability.stream()
				.filter(stats -> stats.sectionId().equals(sectionB.getId()))
				.findFirst()
				.orElseThrow();
		assertThat(sectionBStats.totalSeats()).isEqualTo(2);
		assertThat(sectionBStats.availableSeats()).isEqualTo(2);
	}

	@Test
	void sectionAvailabilityWithUnknownGameThrows() {
		assertThatThrownBy(() -> gameQueryService.getSectionAvailability(999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_NOT_FOUND);
	}

	@Test
	void seatMapReturnsOneRowPerSeatOrderedBySectionAndPosition() {
		Section section = sectionRepository.save(new Section("Infield 601", "R", 30_000));
		seatService.createBulk(section.getId(), new SeatBulkCreateRequest(2, 3));
		GameResponse game = createGame("Seoul Comets", "Busan Gulls");

		List<SeatMapItemResponse> seatMap = gameQueryService.getSeatMap(game.id());

		assertThat(seatMap).hasSize(6);
		assertThat(seatMap.get(0).rowNo()).isEqualTo(1);
		assertThat(seatMap.get(0).seatNo()).isEqualTo(1);
		assertThat(seatMap.get(0).sectionName()).isEqualTo("Infield 601");
		assertThat(seatMap).allSatisfy(item -> assertThat(item.status().name()).isEqualTo("AVAILABLE"));
	}

	@Test
	void seatMapWithUnknownGameThrows() {
		assertThatThrownBy(() -> gameQueryService.getSeatMap(999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_NOT_FOUND);
	}
}
