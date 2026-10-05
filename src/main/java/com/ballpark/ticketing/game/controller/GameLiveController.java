package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.dto.LiveEventResponse;
import com.ballpark.ticketing.game.dto.LiveStateResponse;
import com.ballpark.ticketing.game.live.GameLiveEventHub;
import com.ballpark.ticketing.game.service.GameLiveService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/games/{gameId}/live")
public class GameLiveController {

	private final GameLiveService gameLiveService;
	private final GameLiveEventHub hub;

	public GameLiveController(GameLiveService gameLiveService, GameLiveEventHub hub) {
		this.gameLiveService = gameLiveService;
		this.hub = hub;
	}

	@GetMapping
	public LiveStateResponse snapshot(@PathVariable Long gameId) {
		return gameLiveService.snapshot(gameId);
	}

	/** 경기 기록 전체. 끝난 경기를 다시 보거나 늦게 들어온 사람이 한 번에 받을 때 쓴다. */
	@GetMapping("/events")
	public List<LiveEventResponse> allEvents(@PathVariable Long gameId) {
		return gameLiveService.allEvents(gameId);
	}

	/**
	 * 실시간 스트림. 재접속이면 Last-Event-ID 이후 이벤트를 먼저 보내고, 이미 끝난 경기면 바로 닫는다.
	 * 구독을 리플레이보다 먼저 등록해서 그 사이에 온 이벤트를 놓치지 않는다(절대값이라 중복은 무해하다).
	 */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(
			@PathVariable Long gameId,
			@RequestHeader(name = "Last-Event-ID", required = false) Integer lastEventId) {
		SseEmitter emitter = hub.subscribe(gameId);
		List<LiveEventResponse> replay = gameLiveService.eventsAfter(gameId, lastEventId == null ? 0 : lastEventId);
		replay.forEach(event -> hub.sendTo(emitter, event));
		GameProgress progress = gameLiveService.snapshot(gameId).progress();
		if (progress == GameProgress.FINISHED || progress == GameProgress.CANCELLED) {
			emitter.complete();
		}
		return emitter;
	}
}
