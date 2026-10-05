package com.ballpark.ticketing.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.GameSeatStatus;
import com.ballpark.ticketing.game.Section;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.SeatBulkCreateRequest;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.repository.GameSeatRepository;
import com.ballpark.ticketing.game.repository.SectionRepository;
import com.ballpark.ticketing.game.service.GameService;
import com.ballpark.ticketing.game.service.SeatService;
import com.ballpark.ticketing.payment.PaymentStatus;
import com.ballpark.ticketing.payment.dto.PaymentCreateRequest;
import com.ballpark.ticketing.payment.repository.PaymentRepository;
import com.ballpark.ticketing.payment.service.PaymentService;
import com.ballpark.ticketing.reservation.ReservationStatus;
import com.ballpark.ticketing.reservation.dto.ReservationCreateRequest;
import com.ballpark.ticketing.reservation.dto.ReservationResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class ReservationServiceTest {

	private static final Long USER_ID = 1L;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private SeatService seatService;

	@Autowired
	private GameService gameService;

	@Autowired
	private GameSeatRepository gameSeatRepository;

	@Autowired
	private ReservationService reservationService;

	private GameResponse createOpenGameWithSeats(int rowCount, int seatsPerRow) {
		Section section = sectionRepository.save(new Section("Infield 701", "R", 20_000));
		seatService.createBulk(section.getId(), new SeatBulkCreateRequest(rowCount, seatsPerRow));
		GameResponse game = gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0)));
		gameService.openTicketing(game.id());
		return game;
	}

	private List<Long> gameSeatIdsFor(Long gameId) {
		return gameSeatRepository.findAll().stream()
				.filter(gameSeat -> gameSeat.getGame().getId().equals(gameId))
				.map(GameSeat::getId)
				.toList();
	}

	@Test
	void createsAPendingReservationAndHoldsTheSeats() {
		GameResponse game = createOpenGameWithSeats(1, 2);
		List<Long> gameSeatIds = gameSeatIdsFor(game.id());

		ReservationResponse response = reservationService.create(
				game.id(), USER_ID, new ReservationCreateRequest(gameSeatIds));

		assertThat(response.gameSeatIds()).containsExactlyInAnyOrderElementsOf(gameSeatIds);
		assertThat(response.totalPrice()).isEqualTo(40_000);
		gameSeatIds.forEach(id ->
				assertThat(gameSeatRepository.findById(id).orElseThrow().getStatus())
						.isEqualTo(GameSeatStatus.HELD));
	}

	@Test
	void rejectsReservingBeforeTicketingOpens() {
		Section section = sectionRepository.save(new Section("Infield 702", "R", 20_000));
		seatService.createBulk(section.getId(), new SeatBulkCreateRequest(1, 2));
		GameResponse game = gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0)));
		List<Long> gameSeatIds = gameSeatIdsFor(game.id());

		assertThatThrownBy(() -> reservationService.create(
				game.id(), USER_ID, new ReservationCreateRequest(gameSeatIds)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_NOT_OPEN);
	}

	@Test
	void rejectsAnUnknownGame() {
		assertThatThrownBy(() -> reservationService.create(
				999_999L, USER_ID, new ReservationCreateRequest(List.of(1L))))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_NOT_FOUND);
	}

	@Test
	void rejectsAnUnknownGameSeat() {
		GameResponse game = createOpenGameWithSeats(1, 1);

		assertThatThrownBy(() -> reservationService.create(
				game.id(), USER_ID, new ReservationCreateRequest(List.of(999_999L))))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.GAME_SEAT_NOT_FOUND);
	}

	@Test
	void rejectsASeatThatBelongsToAnotherGame() {
		GameResponse gameA = createOpenGameWithSeats(1, 1);
		// gameB는 등록 시점에 존재하는 모든 좌석(gameA의 구역 포함)에 대해서도 GameSeat를 새로 만든다.
		GameResponse gameB = gameService.create(new GameCreateRequest(
				"Daegu Owls", "Seoul Comets",
				LocalDateTime.of(2026, 11, 2, 18, 30), LocalDateTime.of(2026, 10, 26, 11, 0)));
		List<Long> gameBSeatIds = gameSeatIdsFor(gameB.id());

		assertThatThrownBy(() -> reservationService.create(
				gameA.id(), USER_ID, new ReservationCreateRequest(gameBSeatIds)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.SEAT_GAME_MISMATCH);
	}

	@Test
	void rejectsExceedingFourSeatsPerUserPerGameAcrossReservations() {
		GameResponse game = createOpenGameWithSeats(1, 6);
		List<Long> gameSeatIds = gameSeatIdsFor(game.id());
		reservationService.create(game.id(), USER_ID, new ReservationCreateRequest(gameSeatIds.subList(0, 3)));
		List<Long> extraSeatIds = gameSeatIds.subList(3, 5);

		assertThatThrownBy(() -> reservationService.create(
				game.id(), USER_ID, new ReservationCreateRequest(extraSeatIds)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.TICKET_LIMIT_EXCEEDED);
		extraSeatIds.forEach(id ->
				assertThat(gameSeatRepository.findById(id).orElseThrow().getStatus())
						.isEqualTo(GameSeatStatus.AVAILABLE));
	}

	@Test
	void cancellingReleasesTheSeatsBackToAvailable() {
		GameResponse game = createOpenGameWithSeats(1, 2);
		List<Long> gameSeatIds = gameSeatIdsFor(game.id());
		ReservationResponse created = reservationService.create(
				game.id(), USER_ID, new ReservationCreateRequest(gameSeatIds));

		ReservationResponse cancelled = reservationService.cancel(created.id());

		assertThat(cancelled.status().name()).isEqualTo("CANCELLED");
		gameSeatIds.forEach(id ->
				assertThat(gameSeatRepository.findById(id).orElseThrow().getStatus())
						.isEqualTo(GameSeatStatus.AVAILABLE));
	}

	@Test
	void rejectsCancellingAnUnknownReservation() {
		assertThatThrownBy(() -> reservationService.cancel(999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
	}

	@Test
	void rejectsCancellingAnAlreadyCancelledReservation() {
		GameResponse game = createOpenGameWithSeats(1, 1);
		List<Long> gameSeatIds = gameSeatIdsFor(game.id());
		ReservationResponse created = reservationService.create(
				game.id(), USER_ID, new ReservationCreateRequest(gameSeatIds));
		reservationService.cancel(created.id());

		assertThatThrownBy(() -> reservationService.cancel(created.id()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.RESERVATION_ALREADY_CANCELLED);
	}

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private PaymentRepository paymentRepository;

	@Test
	void cancellingAPendingReservationReleasesTheHeldSeats() {
		GameResponse game = createOpenGameWithSeats(1, 2);
		List<Long> gameSeatIds = gameSeatIdsFor(game.id());
		ReservationResponse reservation = reservationService.create(
				game.id(), USER_ID, new ReservationCreateRequest(gameSeatIds));

		ReservationResponse cancelled = reservationService.cancel(reservation.id());

		assertThat(cancelled.status()).isEqualTo(ReservationStatus.CANCELLED);
		gameSeatIds.forEach(id ->
				assertThat(gameSeatRepository.findById(id).orElseThrow().getStatus())
						.isEqualTo(GameSeatStatus.AVAILABLE));
	}

	@Test
	void cancellingAConfirmedReservationRefundsThePaymentAndFreesTheSeats() {
		GameResponse game = createOpenGameWithSeats(1, 2);
		List<Long> gameSeatIds = gameSeatIdsFor(game.id());
		ReservationResponse reservation = reservationService.create(
				game.id(), USER_ID, new ReservationCreateRequest(gameSeatIds));
		paymentService.pay(reservation.id(), new PaymentCreateRequest(true));

		ReservationResponse cancelled = reservationService.cancel(reservation.id());

		assertThat(cancelled.status()).isEqualTo(ReservationStatus.CANCELLED);
		assertThat(paymentRepository.findByReservation_IdAndStatus(reservation.id(), PaymentStatus.REFUNDED))
				.hasSize(1);
		assertThat(paymentRepository.findByReservation_IdAndStatus(reservation.id(), PaymentStatus.PAID)).isEmpty();
		gameSeatIds.forEach(id ->
				assertThat(gameSeatRepository.findById(id).orElseThrow().getStatus())
						.isEqualTo(GameSeatStatus.AVAILABLE));
	}

	@Test
	void rejectsCancellingAConfirmedReservationAfterTheGameStarted() {
		// 이미 시작한 경기(과거 일정)라 확정 예약 취소가 막혀야 한다.
		Section section = sectionRepository.save(new Section("Infield 703", "R", 20_000));
		seatService.createBulk(section.getId(), new SeatBulkCreateRequest(1, 1));
		GameResponse game = gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2020, 1, 2, 18, 30), LocalDateTime.of(2020, 1, 1, 11, 0)));
		gameService.openTicketing(game.id());
		ReservationResponse reservation = reservationService.create(
				game.id(), USER_ID, new ReservationCreateRequest(gameSeatIdsFor(game.id())));
		paymentService.pay(reservation.id(), new PaymentCreateRequest(true));

		assertThatThrownBy(() -> reservationService.cancel(reservation.id()))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.RESERVATION_NOT_CANCELLABLE);
		assertThat(paymentRepository.findByReservation_IdAndStatus(reservation.id(), PaymentStatus.PAID)).hasSize(1);
	}
}
