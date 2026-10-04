package com.ballpark.ticketing.payment.dto;

import com.ballpark.ticketing.payment.Payment;
import com.ballpark.ticketing.payment.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentResponse(Long id, Long reservationId, long amount, PaymentStatus status, LocalDateTime createdAt) {

	public static PaymentResponse from(Payment payment) {
		return new PaymentResponse(
				payment.getId(), payment.getReservation().getId(), payment.getAmount(), payment.getStatus(),
				payment.getCreatedAt());
	}
}
