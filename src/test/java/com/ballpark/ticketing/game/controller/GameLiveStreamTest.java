package com.ballpark.ticketing.game.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.service.GameLiveService;
import com.ballpark.ticketing.game.service.GameService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * SSE 스트림의 재접속·종료 동작을 확인한다. 끝난 경기는 리플레이 후 바로 닫히므로 비동기 응답이 바로 끝난다.
 * 진행 중 경기의 실시간 전송(커밋 후 push)은 다음 테스트에서 다룬다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class GameLiveStreamTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private GameService gameService;

	@Autowired
	private GameLiveService gameLiveService;

	private long finishedGameWithTwoEvents() {
		GameResponse game = gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2020, 11, 1, 18, 30), LocalDateTime.of(2020, 10, 25, 11, 0)));
		gameLiveService.record(game.id(), new LiveEventCreateRequest(GameEventType.GAME_STARTED, null, null, null, null));
		gameLiveService.record(game.id(), new LiveEventCreateRequest(GameEventType.GAME_FINISHED, null, null, 3, 2));
		return game.id();
	}

	@Test
	void replaysTheWholeLogAndClosesForAFinishedGame() throws Exception {
		long gameId = finishedGameWithTwoEvents();

		MvcResult result = mockMvc.perform(get("/api/games/{id}/live/stream", gameId))
				.andExpect(request().asyncStarted())
				.andReturn();
		String body = mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(body).contains("id:1").contains("id:2").contains("GAME_FINISHED");
	}

	@Test
	void reconnectingAfterTheLastEventSendsNothingNew() throws Exception {
		long gameId = finishedGameWithTwoEvents();

		MvcResult result = mockMvc.perform(get("/api/games/{id}/live/stream", gameId)
						.header("Last-Event-ID", "2"))
				.andExpect(request().asyncStarted())
				.andReturn();
		String body = mockMvc.perform(asyncDispatch(result))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("id:1").doesNotContain("id:2");
	}

	@Test
	void reconnectingAfterTheFirstEventReplaysOnlyTheRest() throws Exception {
		long gameId = finishedGameWithTwoEvents();

		MvcResult result = mockMvc.perform(get("/api/games/{id}/live/stream", gameId)
						.header("Last-Event-ID", "1"))
				.andExpect(request().asyncStarted())
				.andReturn();
		String body = mockMvc.perform(asyncDispatch(result))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("id:1").contains("id:2");
	}
}
