package com.ballpark.ticketing.game.schedule;

import java.time.LocalDateTime;

/** 하루 일정 배치가 정한 경기 한 건. */
public record Matchup(String homeTeam, String awayTeam, LocalDateTime startAt, LocalDateTime plannedEndAt) {
}
