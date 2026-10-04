package com.ballpark.ticketing.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReservationCreateRequest(
		@NotEmpty @Size(max = 4) List<Long> gameSeatIds) {
}
