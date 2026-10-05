package com.ballpark.ticketing.user;

import com.ballpark.ticketing.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그인 사용자. 비밀번호는 평문이 아니라 BCrypt 해시로 저장한다. */
@Getter
@Entity
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false, length = 20)
	private String nickname;

	public AppUser(String email, String passwordHash, String nickname) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.nickname = nickname;
	}
}
