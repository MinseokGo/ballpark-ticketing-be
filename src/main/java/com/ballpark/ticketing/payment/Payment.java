package com.ballpark.ticketing.payment;

import com.ballpark.ticketing.common.entity.BaseTimeEntity;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.reservation.Reservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@Column(nullable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	public Payment(Reservation reservation, long amount) {
		this.reservation = reservation;
		this.amount = amount;
		this.status = PaymentStatus.PENDING;
	}

	public void complete() {
		if (status != PaymentStatus.PENDING) {
			throw new BusinessException(ErrorCode.PAYMENT_NOT_PENDING);
		}
		this.status = PaymentStatus.PAID;
	}

	public void fail() {
		if (status != PaymentStatus.PENDING) {
			throw new BusinessException(ErrorCode.PAYMENT_NOT_PENDING);
		}
		this.status = PaymentStatus.FAILED;
	}

	/** Mock 결제라 환불은 항상 전액이다. 부분 환불은 v1 범위 밖이다. */
	public void refund() {
		if (status != PaymentStatus.PAID) {
			throw new BusinessException(ErrorCode.PAYMENT_NOT_PAID);
		}
		this.status = PaymentStatus.REFUNDED;
	}
}
