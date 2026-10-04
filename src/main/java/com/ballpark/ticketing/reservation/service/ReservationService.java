package com.ballpark.ticketing.reservation.service;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.repository.GameSeatRepository;
import com.ballpark.ticketing.reservation.Reservation;
import com.ballpark.ticketing.reservation.dto.ReservationCreateRequest;
import com.ballpark.ticketing.reservation.dto.ReservationResponse;
import com.ballpark.ticketing.reservation.repository.ReservationRepository;
import com.ballpark.ticketing.reservation.repository.ReservationSeatRepository;
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

	public ReservationService(
			GameRepository gameRepository, GameSeatRepository gameSeatRepository,
			ReservationRepository reservationRepository, ReservationSeatRepository reservationSeatRepository) {
		this.gameRepository = gameRepository;
		this.gameSeatRepository = gameSeatRepository;
		this.reservationRepository = reservationRepository;
		this.reservationSeatRepository = reservationSeatRepository;
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

	public ReservationResponse cancel(Long reservationId) {
		Reservation reservation = findReservation(reservationId);
		reservation.cancel();
		reservation.getReservationSeats().forEach(reservationSeat -> reservationSeat.getGameSeat().release());
		return ReservationResponse.from(reservation);
	}

	private Reservation findReservation(Long reservationId) {
		return reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
	}
}
