package com.ballpark.ticketing.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.dto.ChatMessageResponse;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.user.AppUser;
import com.ballpark.ticketing.user.UserRepository;
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

	@Autowired
	private UserRepository userRepository;

	private Long newUser(String nickname) {
		return userRepository.save(new AppUser("chat-" + nickname + "@example.com", "hash", nickname)).getId();
	}

	private GameResponse createGame() {
		return gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0)));
	}

	@Test
	void keepsMessagesInOrderAndTrimsTheContent() {
		GameResponse game = createGame();

		ChatMessageResponse first = gameChatService.post(game.id(), newUser("가윤"), "  홈런!  ");
		ChatMessageResponse second = gameChatService.post(game.id(), newUser("서준"), "좋다");

		assertThat(first.content()).isEqualTo("홈런!");
		assertThat(gameChatService.recent(game.id()))
				.extracting(ChatMessageResponse::id)
				.containsExactly(first.id(), second.id());
	}

	@Test
	void replaysOnlyMessagesAfterTheGivenId() {
		GameResponse game = createGame();
		ChatMessageResponse first = gameChatService.post(game.id(), newUser("지호"), "하나");
		ChatMessageResponse second = gameChatService.post(game.id(), newUser("하린"), "둘");

		assertThat(gameChatService.since(game.id(), first.id()))
				.extracting(ChatMessageResponse::id)
				.containsExactly(second.id());
	}

	@Test
	void rejectsAnUnknownGame() {
		assertThatThrownBy(() -> gameChatService.post(999_999L, newUser("수아"), "안녕"))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_NOT_FOUND);
	}
}
