package com.ballpark.ticketing.game.dto;

import com.ballpark.ticketing.game.Section;

public record SectionResponse(Long id, String name, String grade, long price) {

	public static SectionResponse from(Section section) {
		return new SectionResponse(section.getId(), section.getName(), section.getGrade(), section.getPrice());
	}
}
