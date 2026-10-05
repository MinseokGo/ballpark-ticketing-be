package com.ballpark.ticketing.payment.service;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.GameSeatStatus;
import com.ballpark.ticketing.game.dto.SeatStatusResponse;
import com.ballpark.ticketing.game.live.SeatStatusHub;
import com.ballpark.ticketing.payment.Payment;
import com.ballpark.ticketing.payment.dto.PaymentCreateRequest;
import com.ballpark.ticketing.payment.dto.PaymentResponse;
import com.ballpark.ticketing.payment.repository.PaymentRepository;
import com.ballpark.ticketing.reservation.Reservation;
import com.ballpark.ticketing.reservation.ReservationSeat;
import com.ballpark.ticketing.reservation.ReservationStatus;
import com.ballpark.ticketing.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

	private final ReservationRepository reservationRepository;
	private final PaymentRepository paymentRepository;

	private final SeatStatusHub seatStatusHub;

	public PaymentService(ReservationRepository reservationRepository, PaymentRepository paymentRepository,
			SeatStatusHub seatStatusHub) {
		this.reservationRepository = reservationRepository;
		this.paymentRepository = paymentRepository;
		this.seatStatusHub = seatStatusHub;
	}

	/**
	 * 실제 PG 연동 없는 Mock 결제. request.success()로 성공/실패를 바로 결정한다.
	 * 성공하면 예약을 확정하고 좌석을 판매 완료로 바꾼다. 실패해도 예약과 좌석은 그대로 둬 재시도할 수 있게 한다(DR-07).
	 */
	public PaymentResponse pay(Long reservationId, PaymentCreateRequest request) {
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
		if (reservation.getStatus() != ReservationStatus.PENDING) {
			throw new BusinessException(ErrorCode.RESERVATION_NOT_PENDING);
		}

		Payment payment = paymentRepository.save(new Payment(reservation, reservation.getTotalPrice()));
		if (request.success()) {
			payment.complete();
			reservation.confirm();
			reservation.getReservationSeats().forEach(reservationSeat -> reservationSeat.getGameSeat().sell());
			seatStatusHub.publishAfterCommit(reservation.getGame().getId(),
					SeatStatusResponse.of(reservation.getReservationSeats().stream().map(ReservationSeat::getGameSeat).toList(),
							GameSeatStatus.SOLD));
		} else {
			payment.fail();
		}
		return PaymentResponse.from(payment);
	}
}
