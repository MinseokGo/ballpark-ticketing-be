package com.ballpark.ticketing.game.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SeatSelectionRequest(
		@NotNull @Size(max = 4) List<Long> gameSeatIds) {
}
