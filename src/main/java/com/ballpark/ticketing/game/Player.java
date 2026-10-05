package com.ballpark.ticketing.game;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 구단 소속 선수. 팀은 구단 이름으로 연결한다(팀 테이블은 v1에 없다). */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
		name = "uk_player_team_number",
		columnNames = {"team_name", "back_number"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String teamName;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false)
	private int backNumber;

	@Column(nullable = false, length = 10)
	private String position;

	public Player(String teamName, String name, int backNumber, String position) {
		if (backNumber <= 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		this.teamName = teamName;
		this.name = name;
		this.backNumber = backNumber;
		this.position = position;
	}
}
