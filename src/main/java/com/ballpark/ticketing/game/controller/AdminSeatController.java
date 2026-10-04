package com.ballpark.ticketing.game.controller;

import com.ballpark.ticketing.game.dto.SeatBulkCreateRequest;
import com.ballpark.ticketing.game.dto.SeatBulkCreateResponse;
import com.ballpark.ticketing.game.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sections/{sectionId}/seats")
public class AdminSeatController {

	private final SeatService seatService;

	public AdminSeatController(SeatService seatService) {
		this.seatService = seatService;
	}

	@PostMapping
	public ResponseEntity<SeatBulkCreateResponse> createSeats(
			@PathVariable Long sectionId, @Valid @RequestBody SeatBulkCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(seatService.createBulk(sectionId, request));
	}
}
