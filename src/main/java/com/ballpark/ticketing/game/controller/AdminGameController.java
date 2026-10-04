package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/games")
public class AdminGameController {

	private final GameService gameService;

	public AdminGameController(GameService gameService) {
		this.gameService = gameService;
	}

	@PostMapping
	public ResponseEntity<GameResponse> createGame(@Valid @RequestBody GameCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(gameService.create(request));
	}
}
