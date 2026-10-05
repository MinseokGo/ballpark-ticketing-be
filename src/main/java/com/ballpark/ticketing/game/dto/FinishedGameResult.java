package com.ballpark.ticketing.game.dto;

/** 종료된 경기의 팀과 최종 점수. 순위 계산에 필요한 값만 직접 조회한다. */
public record FinishedGameResult(String homeTeam, String awayTeam, int homeScore, int awayScore) {
}
