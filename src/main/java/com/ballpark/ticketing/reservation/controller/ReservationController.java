package com.ballpark.ticketing.reservation.controller;

import com.ballpark.ticketing.common.auth.LoginUser;
import com.ballpark.ticketing.reservation.dto.ReservationCreateRequest;
import com.ballpark.ticketing.reservation.dto.ReservationResponse;
import com.ballpark.ticketing.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;

	@PostMapping("/games/{gameId}/reservations")
	public ResponseEntity<ReservationResponse> createReservation(
			@PathVariable Long gameId,
			@LoginUser Long userId,
			@Valid @RequestBody ReservationCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(gameId, userId, request));
	}

	@PostMapping("/reservations/{reservationId}/cancel")
	public ReservationResponse cancelReservation(@PathVariable Long reservationId, @LoginUser Long userId) {
		return reservationService.cancel(reservationId, userId);
	}
}
