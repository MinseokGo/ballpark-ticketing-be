package com.ballpark.ticketing.payment.service;

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
import com.ballpark.ticketing.game.repository.GameSeatRepository;
import com.ballpark.ticketing.game.repository.SectionRepository;
import com.ballpark.ticketing.game.service.GameService;
import com.ballpark.ticketing.game.service.SeatService;
import com.ballpark.ticketing.payment.PaymentStatus;
import com.ballpark.ticketing.payment.dto.PaymentCreateRequest;
import com.ballpark.ticketing.payment.dto.PaymentResponse;
import com.ballpark.ticketing.reservation.ReservationStatus;
import com.ballpark.ticketing.reservation.dto.ReservationCreateRequest;
import com.ballpark.ticketing.reservation.dto.ReservationResponse;
import com.ballpark.ticketing.reservation.repository.ReservationRepository;
import com.ballpark.ticketing.reservation.service.ReservationService;
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
class PaymentServiceTest {

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

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private PaymentService paymentService;

	private ReservationResponse createPendingReservation() {
		Section section = sectionRepository.save(new Section("Infield 801", "R", 25_000));
		seatService.createBulk(section.getId(), new SeatBulkCreateRequest(1, 2));
		GameResponse game = gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0)));
		gameService.openTicketing(game.id());
		List<Long> gameSeatIds = gameSeatRepository.findAll().stream()
				.filter(gameSeat -> gameSeat.getGame().getId().equals(game.id()))
				.map(GameSeat::getId)
				.toList();
		return reservationService.create(game.id(), USER_ID, new ReservationCreateRequest(gameSeatIds));
	}

	@Test
	void successfulPaymentConfirmsReservationAndSellsSeats() {
		ReservationResponse reservation = createPendingReservation();

		PaymentResponse payment = paymentService.pay(reservation.id(), new PaymentCreateRequest(true));

		assertThat(payment.status()).isEqualTo(PaymentStatus.PAID);
		assertThat(payment.amount()).isEqualTo(reservation.totalPrice());
		assertThat(reservationRepository.findById(reservation.id()).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.CONFIRMED);
		reservation.gameSeatIds().forEach(id ->
				assertThat(gameSeatRepository.findById(id).orElseThrow().getStatus())
						.isEqualTo(GameSeatStatus.SOLD));
	}

	@Test
	void failedPaymentKeepsReservationPendingAndSeatsHeld() {
		ReservationResponse reservation = createPendingReservation();

		PaymentResponse payment = paymentService.pay(reservation.id(), new PaymentCreateRequest(false));

		assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
		assertThat(reservationRepository.findById(reservation.id()).orElseThrow().getStatus())
				.isEqualTo(ReservationStatus.PENDING);
		reservation.gameSeatIds().forEach(id ->
				assertThat(gameSeatRepository.findById(id).orElseThrow().getStatus())
						.isEqualTo(GameSeatStatus.HELD));
	}

	@Test
	void rejectsPayingForAnUnknownReservation() {
		assertThatThrownBy(() -> paymentService.pay(999_999L, new PaymentCreateRequest(true)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
	}

	@Test
	void rejectsPayingForAReservationThatIsAlreadyConfirmed() {
		ReservationResponse reservation = createPendingReservation();
		paymentService.pay(reservation.id(), new PaymentCreateRequest(true));

		assertThatThrownBy(() -> paymentService.pay(reservation.id(), new PaymentCreateRequest(true)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.RESERVATION_NOT_PENDING);
	}
}
