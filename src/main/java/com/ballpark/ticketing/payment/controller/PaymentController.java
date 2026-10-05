package com.ballpark.ticketing.payment.controller;

import com.ballpark.ticketing.common.auth.LoginUser;
import com.ballpark.ticketing.payment.dto.PaymentCreateRequest;
import com.ballpark.ticketing.payment.dto.PaymentResponse;
import com.ballpark.ticketing.payment.service.PaymentService;
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
@RequestMapping("/api/reservations/{reservationId}/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping
	public ResponseEntity<PaymentResponse> pay(
			@PathVariable Long reservationId, @LoginUser Long userId, @Valid @RequestBody PaymentCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.pay(reservationId, userId, request));
	}
}
