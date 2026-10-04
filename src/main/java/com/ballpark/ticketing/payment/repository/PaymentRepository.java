package com.ballpark.ticketing.payment.repository;

import com.ballpark.ticketing.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
