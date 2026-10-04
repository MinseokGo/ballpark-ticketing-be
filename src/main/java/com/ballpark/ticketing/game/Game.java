package com.ballpark.ticketing.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String homeTeam;

	@Column(nullable = false, length = 50)
	private String awayTeam;

	@Column(nullable = false)
	private LocalDateTime startAt;

	@Column(nullable = false)
	private LocalDateTime ticketOpenAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private GameStatus status;

	public Game(String homeTeam, String awayTeam, LocalDateTime startAt, LocalDateTime ticketOpenAt) {
		if (!ticketOpenAt.isBefore(startAt)) {
			throw new IllegalArgumentException("ticketOpenAt must be before startAt");
		}
		this.homeTeam = homeTeam;
		this.awayTeam = awayTeam;
		this.startAt = startAt;
		this.ticketOpenAt = ticketOpenAt;
		this.status = GameStatus.SCHEDULED;
	}

	public void openTicketing() {
		if (status != GameStatus.SCHEDULED) {
			throw new IllegalStateException("only a SCHEDULED game can be opened: " + status);
		}
		this.status = GameStatus.OPEN;
	}

	public void closeTicketing() {
		if (status != GameStatus.OPEN) {
			throw new IllegalStateException("only an OPEN game can be closed: " + status);
		}
		this.status = GameStatus.CLOSED;
	}

	public boolean isOpen() {
		return status == GameStatus.OPEN;
	}
}
