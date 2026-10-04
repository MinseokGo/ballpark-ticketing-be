package com.ballpark.ticketing.reservation;

import com.ballpark.ticketing.common.entity.BaseTimeEntity;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameSeat;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status;

	@Column(nullable = false)
	private long totalPrice;

	@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReservationSeat> reservationSeats = new ArrayList<>();

	public Reservation(Long userId, Game game, List<GameSeat> gameSeats, long totalPrice) {
		if (gameSeats.isEmpty()) {
			throw new BusinessException(ErrorCode.RESERVATION_SEATS_EMPTY);
		}
		this.userId = userId;
		this.game = game;
		this.totalPrice = totalPrice;
		this.status = ReservationStatus.PENDING;
		gameSeats.forEach(gameSeat -> reservationSeats.add(new ReservationSeat(this, gameSeat)));
	}

	public List<ReservationSeat> getReservationSeats() {
		return Collections.unmodifiableList(reservationSeats);
	}

	public void confirm() {
		if (status != ReservationStatus.PENDING) {
			throw new BusinessException(ErrorCode.RESERVATION_NOT_PENDING);
		}
		this.status = ReservationStatus.CONFIRMED;
	}

	public void cancel() {
		if (status == ReservationStatus.CANCELLED) {
			throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
		}
		this.status = ReservationStatus.CANCELLED;
	}
}
