package com.ballpark.ticketing.reservation.service;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.repository.GameSeatRepository;
import com.ballpark.ticketing.payment.Payment;
import com.ballpark.ticketing.payment.PaymentStatus;
import com.ballpark.ticketing.payment.repository.PaymentRepository;
import com.ballpark.ticketing.reservation.Reservation;
import com.ballpark.ticketing.reservation.ReservationStatus;
import com.ballpark.ticketing.reservation.dto.ReservationCreateRequest;
import com.ballpark.ticketing.reservation.dto.ReservationResponse;
import com.ballpark.ticketing.reservation.repository.ReservationRepository;
import com.ballpark.ticketing.reservation.repository.ReservationSeatRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationService {

	private static final int MAX_SEATS_PER_USER_PER_GAME = 4;

	private final GameRepository gameRepository;
	private final GameSeatRepository gameSeatRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationSeatRepository reservationSeatRepository;
	private final PaymentRepository paymentRepository;
	private final Clock clock;

	public ReservationService(
			GameRepository gameRepository, GameSeatRepository gameSeatRepository,
			ReservationRepository reservationRepository, ReservationSeatRepository reservationSeatRepository,
			PaymentRepository paymentRepository, Clock clock) {
		this.gameRepository = gameRepository;
		this.gameSeatRepository = gameSeatRepository;
		this.reservationRepository = reservationRepository;
		this.reservationSeatRepository = reservationSeatRepository;
		this.paymentRepository = paymentRepository;
		this.clock = clock;
	}

	public ReservationResponse create(Long gameId, Long userId, ReservationCreateRequest request) {
		Game game = gameRepository.findById(gameId).orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
		if (!game.isOpen()) {
			throw new BusinessException(ErrorCode.GAME_NOT_OPEN);
		}

		List<Long> gameSeatIds = request.gameSeatIds();
		List<GameSeat> gameSeats = gameSeatRepository.findAllById(gameSeatIds);
		if (gameSeats.size() != gameSeatIds.size()) {
			throw new BusinessException(ErrorCode.GAME_SEAT_NOT_FOUND);
		}
		boolean seatFromAnotherGame = gameSeats.stream()
				.anyMatch(gameSeat -> !gameSeat.getGame().getId().equals(gameId));
		if (seatFromAnotherGame) {
			throw new BusinessException(ErrorCode.SEAT_GAME_MISMATCH);
		}

		long alreadyReserved = reservationSeatRepository.countActiveByUserAndGame(userId, gameId);
		if (alreadyReserved + gameSeats.size() > MAX_SEATS_PER_USER_PER_GAME) {
			throw new BusinessException(ErrorCode.TICKET_LIMIT_EXCEEDED);
		}

		gameSeats.forEach(GameSeat::hold);
		long totalPrice = gameSeats.stream()
				.mapToLong(gameSeat -> gameSeat.getSeat().getSection().getPrice())
				.sum();

		Reservation reservation = reservationRepository.save(new Reservation(userId, game, gameSeats, totalPrice));
		return ReservationResponse.from(reservation);
	}

	/**
	 * 결제 대기 예약은 좌석 선점만 풀고, 확정 예약은 결제를 전액 환불하고 좌석을 다시 판매 가능으로 돌린다.
	 * 확정 예약은 경기 시작 전까지만 취소할 수 있다(시작 후에는 환불 정책이 없다).
	 */
	public ReservationResponse cancel(Long reservationId) {
		Reservation reservation = findReservation(reservationId);
		if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
			if (reservation.getGame().hasStarted(LocalDateTime.now(clock))) {
				throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELLABLE);
			}
			reservation.cancel();
			paymentRepository.findByReservation_IdAndStatus(reservationId, PaymentStatus.PAID)
					.forEach(Payment::refund);
			reservation.getReservationSeats().forEach(reservationSeat -> reservationSeat.getGameSeat().refund());
		} else {
			reservation.cancel();
			reservation.getReservationSeats().forEach(reservationSeat -> reservationSeat.getGameSeat().release());
		}
		return ReservationResponse.from(reservation);
	}

	private Reservation findReservation(Long reservationId) {
		return reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
	}
}
