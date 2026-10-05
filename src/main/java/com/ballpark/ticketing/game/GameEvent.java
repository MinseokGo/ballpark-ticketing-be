package com.ballpark.ticketing.game;

import com.ballpark.ticketing.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 경기 진행 이벤트 로그. 한 경기 안에서 seq가 1부터 올라간다. 현재 상태는 Game 필드에 있고,
 * 이 로그는 실시간 스트림 재접속(Last-Event-ID)과 경기 결과 기록에 쓴다.
 */
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
		name = "uk_game_event_game_seq",
		columnNames = {"game_id", "seq"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameEvent extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Column(nullable = false)
	private int seq;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	private GameEventType type;

	private Integer inning;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(length = 10)
	private Half half;

	@Column(nullable = false)
	private int homeScore;

	@Column(nullable = false)
	private int awayScore;

	// 득점 등 선수가 관여한 이벤트에만 붙는다(없으면 null).
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id")
	private Player player;

	// 선수 기록의 종류. 예: 안타, 홈런. 자유 문자열 대신 짧은 값만 받는다.
	@Column(length = 20)
	private String detail;

	public GameEvent(Game game, int seq, GameEventType type, Integer inning, Half half, int homeScore, int awayScore) {
		this(game, seq, type, inning, half, homeScore, awayScore, null, null);
	}

	public GameEvent(Game game, int seq, GameEventType type, Integer inning, Half half, int homeScore, int awayScore,
			Player player, String detail) {
		this.game = game;
		this.seq = seq;
		this.type = type;
		this.inning = inning;
		this.half = half;
		this.homeScore = homeScore;
		this.awayScore = awayScore;
		this.player = player;
		this.detail = detail;
	}
}
