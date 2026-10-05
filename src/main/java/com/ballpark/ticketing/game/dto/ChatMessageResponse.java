package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.GameChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
		Long id,
		Long gameId,
		Long userId,
		String content,
		LocalDateTime createdAt) {

	public static ChatMessageResponse from(GameChatMessage message) {
		return new ChatMessageResponse(
				message.getId(), message.getGame().getId(), message.getUserId(), message.getContent(),
				message.getCreatedAt());
	}
}
