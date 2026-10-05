package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.common.auth.LoginUser;
import com.ballpark.ticketing.game.dto.ChatMessageCreateRequest;
import com.ballpark.ticketing.game.dto.ChatMessageResponse;
import com.ballpark.ticketing.game.live.GameChatHub;
import com.ballpark.ticketing.game.service.GameChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/games/{gameId}/chat")
public class GameChatController {

	private final GameChatService gameChatService;
	private final GameChatHub hub;

	public GameChatController(GameChatService gameChatService, GameChatHub hub) {
		this.gameChatService = gameChatService;
		this.hub = hub;
	}

	@GetMapping("/messages")
	public List<ChatMessageResponse> recent(@PathVariable Long gameId) {
		return gameChatService.recent(gameId);
	}

	@PostMapping("/messages")
	@ResponseStatus(HttpStatus.CREATED)
	public ChatMessageResponse post(
			@PathVariable Long gameId,
			@LoginUser Long userId,
			@Valid @RequestBody ChatMessageCreateRequest request) {
		return gameChatService.post(gameId, userId, request.content());
	}

	/**
	 * 채팅 스트림. Last-Event-ID가 있으면 그 뒤 메시지를 먼저 보내서, 목록을 받은 뒤 연결하기 전에 온 메시지를 채운다.
	 * 구독을 리플레이보다 먼저 등록한다. 중복은 클라이언트가 메시지 번호로 거른다.
	 */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(
			@PathVariable Long gameId,
			@RequestHeader(name = "Last-Event-ID", required = false) Long lastEventId) {
		SseEmitter emitter = hub.subscribe(gameId);
		if (lastEventId != null) {
			gameChatService.since(gameId, lastEventId).forEach(message -> hub.sendTo(emitter, message));
		}
		return emitter;
	}
}
