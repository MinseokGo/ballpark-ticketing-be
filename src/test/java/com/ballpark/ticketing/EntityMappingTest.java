package com.ballpark.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ballpark.ticketing.common.config.JpaAuditingConfig;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.Seat;
import com.ballpark.ticketing.game.Section;
import com.ballpark.ticketing.payment.Payment;
import com.ballpark.ticketing.reservation.Reservation;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class EntityMappingTest {

	@Autowired
	private TestEntityManager em;

	@Test
	void persistsAFullReservationAndPayment() {
		Section section = em.persist(new Section("Infield 101", "R", 30_000));
		Seat seat = em.persist(new Seat(section, 1, 1));
		Game game = em.persist(new Game("Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 10, 10, 18, 30), LocalDateTime.of(2026, 10, 3, 11, 0)));
		GameSeat gameSeat = em.persist(new GameSeat(game, seat));
		Reservation reservation = em.persist(new Reservation(1L, game, List.of(gameSeat), 30_000));
		Payment payment = em.persist(new Payment(reservation, 30_000));
		em.flush();
		em.clear();

		Reservation found = em.find(Reservation.class, reservation.getId());
		assertThat(found.getReservationSeats()).hasSize(1);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(em.find(Payment.class, payment.getId()).getCreatedAt()).isNotNull();
	}

	@Test
	void rejectsTheSameSeatTwiceForOneGame() {
		Section section = em.persist(new Section("Infield 102", "R", 30_000));
		Seat seat = em.persist(new Seat(section, 1, 1));
		Game game = em.persist(new Game("Seoul Comets", "Daegu Owls",
				LocalDateTime.of(2026, 10, 11, 18, 30), LocalDateTime.of(2026, 10, 4, 11, 0)));
		em.persist(new GameSeat(game, seat));

		assertThatThrownBy(() -> em.persistAndFlush(new GameSeat(game, seat)))
				.isInstanceOf(ConstraintViolationException.class);
	}
}
