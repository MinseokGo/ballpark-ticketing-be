package com.ballpark.ticketing.reservation.controller;

import com.ballpark.ticketing.common.auth.LoginUser;
import com.ballpark.ticketing.reservation.dto.MyReservationResponse;
import com.ballpark.ticketing.reservation.service.ReservationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MyReservationController {

	private final ReservationService reservationService;

	public MyReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@GetMapping("/reservations")
	public List<MyReservationResponse> myReservations(@LoginUser Long userId) {
		return reservationService.myReservations(userId);
	}
}
