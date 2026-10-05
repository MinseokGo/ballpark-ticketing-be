package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.common.auth.LoginUser;
import com.ballpark.ticketing.game.dto.SeatSelectionRequest;
import com.ballpark.ticketing.game.live.SeatStatusHub;
import com.ballpark.ticketing.game.service.SeatSelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/games/{gameId}/seats")
@RequiredArgsConstructor
public class GameSeatStreamController {

	private final SeatStatusHub seatStatusHub;
	private final SeatSelectionService seatSelectionService;

	/** 좌석 상태 변경과 고르는 중 목록을 보내는 스트림. 연결하면 지금 목록을 먼저 받는다. */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@PathVariable Long gameId) {
		SseEmitter emitter = seatStatusHub.subscribe(gameId);
		seatStatusHub.sendSelections(emitter, seatSelectionService.snapshot(gameId));
		return emitter;
	}

	/** 내가 지금 고르는 좌석을 알린다. 30초 동안 갱신이 없으면 사라진다. 예매를 잡지 않는다. */
	@PostMapping("/selection")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void select(@PathVariable Long gameId, @LoginUser Long userId,
			@Valid @RequestBody SeatSelectionRequest request) {
		seatSelectionService.update(gameId, userId, request.gameSeatIds());
	}
}
