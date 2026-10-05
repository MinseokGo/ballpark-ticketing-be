package com.ballpark.ticketing.payment.repository;

import com.ballpark.ticketing.payment.Payment;
import com.ballpark.ticketing.payment.PaymentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	List<Payment> findByReservation_IdAndStatus(Long reservationId, PaymentStatus status);
}
