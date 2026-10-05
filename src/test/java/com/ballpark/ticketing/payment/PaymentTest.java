package com.ballpark.ticketing.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.Seat;
import com.ballpark.ticketing.game.Section;
import com.ballpark.ticketing.reservation.Reservation;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentTest {

	private final Game game = new Game("Seoul Comets", "Busan Gulls",
			LocalDateTime.of(2026, 10, 10, 18, 30), LocalDateTime.of(2026, 10, 3, 11, 0));
	private final GameSeat gameSeat = new GameSeat(game, new Seat(new Section("Infield 101", "R", 30_000), 1, 1));
	private final Reservation reservation = new Reservation(1L, game, List.of(gameSeat), 30_000);

	@Test
	void completesAPendingPayment() {
		Payment payment = new Payment(reservation, 30_000);

		payment.complete();

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
	}

	@Test
	void failsAPendingPayment() {
		Payment payment = new Payment(reservation, 30_000);

		payment.fail();

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
	}

	@Test
	void rejectsCompletingAPaymentThatIsNotPending() {
		Payment payment = new Payment(reservation, 30_000);
		payment.complete();

		assertThatThrownBy(payment::complete)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_PENDING);
	}

	@Test
	void rejectsFailingAPaymentThatIsNotPending() {
		Payment payment = new Payment(reservation, 30_000);
		payment.fail();

		assertThatThrownBy(payment::fail)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_PENDING);
	}

	@Test
	void refundsAPaidPayment() {
		Payment payment = new Payment(reservation, 30_000);
		payment.complete();

		payment.refund();

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
	}

	@Test
	void rejectsRefundingAPaymentThatIsNotPaid() {
		Payment payment = new Payment(reservation, 30_000);

		assertThatThrownBy(payment::refund)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_NOT_PAID);
	}
}
