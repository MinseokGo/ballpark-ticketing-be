package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.game.dto.StandingResponse;
import com.ballpark.ticketing.game.service.StandingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/standings")
@RequiredArgsConstructor
public class StandingController {

	private final StandingService standingService;

	@GetMapping
	public List<StandingResponse> getStandings() {
		return standingService.standings();
	}
}
