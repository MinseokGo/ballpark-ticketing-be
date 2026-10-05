package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.game.live.SeatStatusHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/games/{gameId}/seats")
public class GameSeatStreamController {

	private final SeatStatusHub seatStatusHub;

	public GameSeatStreamController(SeatStatusHub seatStatusHub) {
		this.seatStatusHub = seatStatusHub;
	}

	/** 좌석 상태 변경 스트림. 연결이 끊기면 클라이언트가 좌석표를 다시 받는다. */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@PathVariable Long gameId) {
		return seatStatusHub.subscribe(gameId);
	}
}
