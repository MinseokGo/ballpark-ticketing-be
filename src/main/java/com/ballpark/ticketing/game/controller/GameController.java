package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.common.dto.PageResponse;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.GameSummaryResponse;
import com.ballpark.ticketing.game.dto.SeatMapItemResponse;
import com.ballpark.ticketing.game.dto.SectionAvailabilityResponse;
import com.ballpark.ticketing.game.service.GameQueryService;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

	private final GameQueryService gameQueryService;

	public GameController(GameQueryService gameQueryService) {
		this.gameQueryService = gameQueryService;
	}

	@GetMapping
	public PageResponse<GameSummaryResponse> getGames(
			@PageableDefault(size = 20, sort = "startAt") Pageable pageable) {
		return gameQueryService.listGames(pageable);
	}

	@GetMapping("/{gameId}")
	public GameResponse getGame(@PathVariable Long gameId) {
		return gameQueryService.getGame(gameId);
	}

	@GetMapping("/{gameId}/sections")
	public List<SectionAvailabilityResponse> getSectionAvailability(@PathVariable Long gameId) {
		return gameQueryService.getSectionAvailability(gameId);
	}

	@GetMapping("/{gameId}/seats")
	public List<SeatMapItemResponse> getSeatMap(@PathVariable Long gameId) {
		return gameQueryService.getSeatMap(gameId);
	}
}
