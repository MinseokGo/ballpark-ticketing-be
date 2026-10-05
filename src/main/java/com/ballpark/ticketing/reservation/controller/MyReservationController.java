package com.ballpark.ticketing.reservation.controller;

import com.ballpark.ticketing.common.auth.LoginUser;
import com.ballpark.ticketing.reservation.dto.MyReservationResponse;
import com.ballpark.ticketing.reservation.service.ReservationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyReservationController {

	private final ReservationService reservationService;

	@GetMapping("/reservations")
	public List<MyReservationResponse> myReservations(@LoginUser Long userId) {
		return reservationService.myReservations(userId);
	}
}
