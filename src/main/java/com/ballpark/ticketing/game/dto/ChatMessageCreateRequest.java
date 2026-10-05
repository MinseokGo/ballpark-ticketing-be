package com.ballpark.ticketing.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageCreateRequest(
		@NotBlank @Size(max = 200) String content) {
}
