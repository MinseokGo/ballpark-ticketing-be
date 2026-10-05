package com.ballpark.ticketing.user.dto;

public record AuthResponse(String token, UserResponse user) {
}
