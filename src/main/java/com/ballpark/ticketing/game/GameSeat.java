package com.ballpark.ticketing.game;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
		name = "uk_game_seat_game_seat",
		columnNames = {"game_id", "seat_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSeat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seat_id", nullable = false)
	private Seat seat;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private GameSeatStatus status;

	public GameSeat(Game game, Seat seat) {
		this.game = game;
		this.seat = seat;
		this.status = GameSeatStatus.AVAILABLE;
	}

	public void hold() {
		if (status != GameSeatStatus.AVAILABLE) {
			throw new IllegalStateException("only an AVAILABLE seat can be held: " + status);
		}
		this.status = GameSeatStatus.HELD;
	}

	public void sell() {
		if (status != GameSeatStatus.HELD) {
			throw new IllegalStateException("only a HELD seat can be sold: " + status);
		}
		this.status = GameSeatStatus.SOLD;
	}

	public void release() {
		if (status == GameSeatStatus.AVAILABLE) {
			throw new IllegalStateException("seat is already AVAILABLE");
		}
		this.status = GameSeatStatus.AVAILABLE;
	}
}
