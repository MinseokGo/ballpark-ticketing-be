package com.ballpark.ticketing.game.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ballpark.ticketing.common.dto.PageResponse;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameSeatStatus;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.GameSummaryResponse;
import com.ballpark.ticketing.game.dto.SeatMapItemResponse;
import com.ballpark.ticketing.game.dto.SectionAvailabilityResponse;
import com.ballpark.ticketing.game.service.GameQueryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
class GameControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GameQueryService gameQueryService;

	@Test
	void getGamesReturnsPagedSummaries() throws Exception {
		Game game = new Game("Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0));
		GameSummaryResponse summary = GameSummaryResponse.from(game);
		given(gameQueryService.listGames(any(), any())).willReturn(new PageResponse<>(List.of(summary), 0, 20, 1, 1));

		mockMvc.perform(get("/api/games"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].homeTeam").value("Seoul Comets"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getGameReturnsDetail() throws Exception {
		Game game = new Game("Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0));
		given(gameQueryService.getGame(1L)).willReturn(GameResponse.of(game, 50));

		mockMvc.perform(get("/api/games/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gameSeatCount").value(50));
	}

	@Test
	void getGameWithUnknownIdReturnsNotFound() throws Exception {
		given(gameQueryService.getGame(999L)).willThrow(new BusinessException(ErrorCode.GAME_NOT_FOUND));

		mockMvc.perform(get("/api/games/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("GAME-004"));
	}

	@Test
	void getSectionAvailabilityReturnsList() throws Exception {
		given(gameQueryService.getSectionAvailability(eq(1L)))
				.willReturn(List.of(new SectionAvailabilityResponse(1L, "Infield 101", "R", 30_000, 10, 7, 2, 1)));

		mockMvc.perform(get("/api/games/1/sections"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Infield 101"))
				.andExpect(jsonPath("$[0].availableSeats").value(7));
	}

	@Test
	void getSeatMapReturnsList() throws Exception {
		given(gameQueryService.getSeatMap(eq(1L)))
				.willReturn(List.of(
						new SeatMapItemResponse(1L, 1L, 1, 1, 1L, "Infield 101", "R", GameSeatStatus.AVAILABLE)));

		mockMvc.perform(get("/api/games/1/seats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].rowNo").value(1))
				.andExpect(jsonPath("$[0].status").value("AVAILABLE"));
	}
}
