package com.ballpark.ticketing.game;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String homeTeam;

	@Column(nullable = false, length = 50)
	private String awayTeam;

	@Column(nullable = false)
	private LocalDateTime startAt;

	@Column(nullable = false)
	private LocalDateTime ticketOpenAt;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private GameStatus status;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private GameProgress progress;

	private Integer inning;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(length = 10)
	private Half half;

	@Column(nullable = false)
	private int homeScore;

	@Column(nullable = false)
	private int awayScore;

	// 경기 안에서 이벤트 번호를 매긴다. 이벤트를 기록할 때 game 행을 잠그고 올리므로 번호가 겹치지 않는다.
	@Column(nullable = false)
	private int eventSeq;

	// 예정 종료 시각. 일정 배치가 만든 경기에만 있다. 이 시각이 지나면 중계 생성기가 경기를 끝낸다.
	private LocalDateTime plannedEndAt;

	public Game(String homeTeam, String awayTeam, LocalDateTime startAt, LocalDateTime ticketOpenAt) {
		if (!ticketOpenAt.isBefore(startAt)) {
			throw new BusinessException(ErrorCode.INVALID_GAME_SCHEDULE);
		}
		this.homeTeam = homeTeam;
		this.awayTeam = awayTeam;
		this.startAt = startAt;
		this.ticketOpenAt = ticketOpenAt;
		this.status = GameStatus.SCHEDULED;
		this.progress = GameProgress.NOT_STARTED;
	}

	public void openTicketing() {
		if (status != GameStatus.SCHEDULED) {
			throw new BusinessException(ErrorCode.GAME_NOT_SCHEDULED);
		}
		this.status = GameStatus.OPEN;
	}

	public void closeTicketing() {
		if (status != GameStatus.OPEN) {
			throw new BusinessException(ErrorCode.GAME_NOT_OPEN);
		}
		this.status = GameStatus.CLOSED;
	}

	public boolean isOpen() {
		return status == GameStatus.OPEN;
	}

	/** 예정 종료 시각을 정한다(일정 배치가 쓴다). */
	public void planEnd(LocalDateTime plannedEndAt) {
		this.plannedEndAt = plannedEndAt;
	}

	/** 예정 종료 시각이 있고 그 시각이 지났는지. */
	public boolean isPastPlannedEnd(LocalDateTime now) {
		return plannedEndAt != null && !now.isBefore(plannedEndAt);
	}

	/** 경기 시작 시각에 도달했는지. 확정 예약의 취소 가능 여부를 가른다. */
	public boolean hasStarted(LocalDateTime now) {
		return !now.isBefore(startAt);
	}

	/**
	 * 시작 시각이 지난 뒤에만 시작한다. 예정 전에 중계가 올라가는 일을 여기서 막아서, 관리자 API든 생성기든 같은 규칙을 따른다.
	 */
	public void start(LocalDateTime now) {
		if (progress == GameProgress.LIVE) {
			throw new BusinessException(ErrorCode.GAME_ALREADY_STARTED);
		}
		requireNotEnded();
		if (!hasStarted(now)) {
			throw new BusinessException(ErrorCode.GAME_NOT_YET_STARTABLE);
		}
		this.progress = GameProgress.LIVE;
		this.inning = 1;
		this.half = Half.TOP;
	}

	/** 이닝은 앞으로만 간다. 같은 회의 초에서 말로 넘어가는 것은 허용한다. */
	public void changeInning(int inning, Half half) {
		requireLive();
		if (position(inning, half) <= position(this.inning, this.half)) {
			throw new BusinessException(ErrorCode.INNING_NOT_ADVANCED);
		}
		this.inning = inning;
		this.half = half;
	}

	/** 점수는 줄어들 수 없다. 잘못 입력한 점수는 correctScore로 고친다. */
	public void advanceScore(int homeScore, int awayScore) {
		requireLive();
		requireNotDecreased(homeScore, awayScore);
		this.homeScore = homeScore;
		this.awayScore = awayScore;
	}

	public void correctScore(int homeScore, int awayScore) {
		requireLive();
		this.homeScore = homeScore;
		this.awayScore = awayScore;
	}

	public void finish(int homeScore, int awayScore) {
		requireLive();
		requireNotDecreased(homeScore, awayScore);
		this.homeScore = homeScore;
		this.awayScore = awayScore;
		this.progress = GameProgress.FINISHED;
	}

	/** 플레이 기록은 점수나 이닝을 바꾸지 않는다. 진행 중인 경기에서만 받는다. */
	public void recordPlay() {
		requireLive();
	}

	public void cancel() {
		requireNotEnded();
		this.progress = GameProgress.CANCELLED;
	}

	/** 종료된 경기의 승부. 종료 전이나 취소된 경기는 null이다. */
	public Winner winner() {
		if (progress != GameProgress.FINISHED) {
			return null;
		}
		if (homeScore == awayScore) {
			return Winner.DRAW;
		}
		return homeScore > awayScore ? Winner.HOME : Winner.AWAY;
	}

	public int nextEventSeq() {
		this.eventSeq++;
		return this.eventSeq;
	}

	private void requireLive() {
		requireNotEnded();
		if (progress != GameProgress.LIVE) {
			throw new BusinessException(ErrorCode.GAME_NOT_LIVE);
		}
	}

	private void requireNotEnded() {
		if (progress == GameProgress.FINISHED || progress == GameProgress.CANCELLED) {
			throw new BusinessException(ErrorCode.GAME_ALREADY_ENDED);
		}
	}

	private void requireNotDecreased(int homeScore, int awayScore) {
		if (homeScore < this.homeScore || awayScore < this.awayScore) {
			throw new BusinessException(ErrorCode.SCORE_DECREASED);
		}
	}

	private static int position(Integer inning, Half half) {
		return inning * 2 + (half == Half.BOTTOM ? 1 : 0);
	}
}
