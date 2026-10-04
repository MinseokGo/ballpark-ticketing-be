package com.ballpark.ticketing.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record SectionCreateRequest(
		@NotBlank String name,
		@NotBlank String grade,
		@PositiveOrZero long price) {
}
