package com.ballpark.ticketing.game;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	@Column(nullable = false, length = 20)
	private String grade;

	@Column(nullable = false)
	private long price;

	public Section(String name, String grade, long price) {
		if (price < 0) {
			throw new BusinessException(ErrorCode.INVALID_SECTION_PRICE);
		}
		this.name = name;
		this.grade = grade;
		this.price = price;
	}
}
