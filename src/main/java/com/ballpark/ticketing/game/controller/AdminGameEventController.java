package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.dto.LiveEventResponse;
import com.ballpark.ticketing.game.service.GameLiveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/games/{gameId}/events")
public class AdminGameEventController {

	private final GameLiveService gameLiveService;

	public AdminGameEventController(GameLiveService gameLiveService) {
		this.gameLiveService = gameLiveService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LiveEventResponse record(@PathVariable Long gameId, @Valid @RequestBody LiveEventCreateRequest request) {
		return gameLiveService.record(gameId, request);
	}
}
