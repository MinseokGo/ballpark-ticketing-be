package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameChatMessage;
import com.ballpark.ticketing.game.dto.ChatMessageResponse;
import com.ballpark.ticketing.game.live.GameChatHub;
import com.ballpark.ticketing.game.repository.GameChatMessageRepository;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.user.AppUser;
import com.ballpark.ticketing.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GameChatService {

	private final GameRepository gameRepository;
	private final GameChatMessageRepository messageRepository;
	private final GameChatHub hub;
	private final UserRepository userRepository;

	public ChatMessageResponse post(Long gameId, Long userId, String content) {
		Game game = findGame(gameId);
		String nickname = userRepository.findById(userId)
				.map(AppUser::getNickname)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		GameChatMessage message = messageRepository.save(new GameChatMessage(game, userId, nickname, content.trim()));
		ChatMessageResponse response = ChatMessageResponse.from(message);
		hub.publishAfterCommit(gameId, response);
		return response;
	}

	/** 최근 메시지 50개를 오래된 순서로 돌려준다. */
	@Transactional(readOnly = true)
	public List<ChatMessageResponse> recent(Long gameId) {
		findGame(gameId);
		List<ChatMessageResponse> latest = messageRepository.findTop50ByGame_IdOrderByIdDesc(gameId).stream()
				.map(ChatMessageResponse::from)
				.toList();
		return latest.reversed();
	}

	/** 재접속 복구용. lastId보다 큰 메시지를 번호 순서대로 돌려준다. */
	@Transactional(readOnly = true)
	public List<ChatMessageResponse> since(Long gameId, long lastId) {
		findGame(gameId);
		return messageRepository.findByGame_IdAndIdGreaterThanOrderByIdAsc(gameId, lastId).stream()
				.map(ChatMessageResponse::from)
				.toList();
	}

	private Game findGame(Long gameId) {
		return gameRepository.findById(gameId).orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
	}
}
