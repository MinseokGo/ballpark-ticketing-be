package com.ballpark.ticketing.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.dto.ChatMessageResponse;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
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
class GameChatServiceTest {

	@Autowired
	private GameService gameService;

	@Autowired
	private GameChatService gameChatService;

	private GameResponse createGame() {
		return gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0)));
	}

	@Test
	void keepsMessagesInOrderAndTrimsTheContent() {
		GameResponse game = createGame();

		ChatMessageResponse first = gameChatService.post(game.id(), 1L, "  홈런!  ");
		ChatMessageResponse second = gameChatService.post(game.id(), 2L, "좋다");

		assertThat(first.content()).isEqualTo("홈런!");
		assertThat(gameChatService.recent(game.id()))
				.extracting(ChatMessageResponse::id)
				.containsExactly(first.id(), second.id());
	}

	@Test
	void replaysOnlyMessagesAfterTheGivenId() {
		GameResponse game = createGame();
		ChatMessageResponse first = gameChatService.post(game.id(), 1L, "하나");
		ChatMessageResponse second = gameChatService.post(game.id(), 2L, "둘");

		assertThat(gameChatService.since(game.id(), first.id()))
				.extracting(ChatMessageResponse::id)
				.containsExactly(second.id());
	}

	@Test
	void rejectsAnUnknownGame() {
		assertThatThrownBy(() -> gameChatService.post(999_999L, 1L, "안녕"))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_NOT_FOUND);
	}
}
