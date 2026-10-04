package com.ballpark.ticketing.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record GameCreateRequest(
		@NotBlank String homeTeam,
		@NotBlank String awayTeam,
		@NotNull LocalDateTime startAt,
		@NotNull LocalDateTime ticketOpenAt) {
}
