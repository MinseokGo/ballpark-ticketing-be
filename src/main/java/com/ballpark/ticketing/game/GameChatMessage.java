package com.ballpark.ticketing.game;

import com.ballpark.ticketing.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 경기별 실시간 채팅 메시지. 보낸 사람은 로그인 사용자 번호와 닉네임이다. */
@Getter
@Entity
@Table(indexes = @Index(name = "idx_game_chat_game_id", columnList = "game_id, id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameChatMessage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, length = 200)
	private String content;

	// 보낼 때의 닉네임을 같이 저장한다. 예전 메시지(닉네임 없음)는 null이다.
	@Column(length = 20)
	private String nickname;

	public GameChatMessage(Game game, Long userId, String nickname, String content) {
		this.game = game;
		this.userId = userId;
		this.nickname = nickname;
		this.content = content;
	}
}
