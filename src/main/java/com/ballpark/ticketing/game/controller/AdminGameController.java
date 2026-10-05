package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/games")
@RequiredArgsConstructor
public class AdminGameController {

	private final GameService gameService;

	@PostMapping
	public ResponseEntity<GameResponse> createGame(@Valid @RequestBody GameCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(gameService.create(request));
	}

	@PatchMapping("/{gameId}/open")
	public GameResponse openTicketing(@PathVariable Long gameId) {
		return gameService.openTicketing(gameId);
	}
}
