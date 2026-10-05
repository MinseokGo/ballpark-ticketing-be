package com.ballpark.ticketing.user.dto;

import com.ballpark.ticketing.user.AppUser;

public record UserResponse(Long id, String email, String nickname) {

	public static UserResponse from(AppUser user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getNickname());
	}
}
