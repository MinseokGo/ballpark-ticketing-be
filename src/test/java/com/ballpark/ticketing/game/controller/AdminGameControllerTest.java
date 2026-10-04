package com.ballpark.ticketing.game.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.service.GameService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminGameController.class)
class AdminGameControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GameService gameService;

	@Test
	void createsGameAndItsGameSeats() throws Exception {
		Game game = new Game("Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0));
		given(gameService.create(any())).willReturn(GameResponse.of(game, 22_000));

		mockMvc.perform(post("/api/admin/games")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"homeTeam": "Seoul Comets", "awayTeam": "Busan Gulls",
								 "startAt": "2026-11-01T18:30:00", "ticketOpenAt": "2026-10-25T11:00:00"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.homeTeam").value("Seoul Comets"))
				.andExpect(jsonPath("$.gameSeatCount").value(22_000));
	}

	@Test
	void rejectsTicketOpenAtAfterStartAt() throws Exception {
		mockMvc.perform(post("/api/admin/games")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"homeTeam": "Seoul Comets", "awayTeam": "Busan Gulls",
								 "startAt": "2026-11-01T18:30:00", "ticketOpenAt": null}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-001"));
	}

	@Test
	void opensTicketing() throws Exception {
		Game game = new Game("Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0));
		game.openTicketing();
		given(gameService.openTicketing(eq(1L))).willReturn(GameResponse.of(game, 100));

		mockMvc.perform(patch("/api/admin/games/1/open"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	void openingAnUnknownGameReturnsNotFound() throws Exception {
		given(gameService.openTicketing(eq(999L))).willThrow(new BusinessException(ErrorCode.GAME_NOT_FOUND));

		mockMvc.perform(patch("/api/admin/games/999/open"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("GAME-004"));
	}
}
