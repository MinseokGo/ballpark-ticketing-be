package com.ballpark.ticketing.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 경기 시각과 생성 시각은 한국 현지 시각(LocalDateTime)으로 다룬다. 서버나 DB가 어느 시간대에 있든
 * 값이 어긋나지 않도록, 시각을 읽는 곳은 전부 이 Clock을 거친다(ISSUE-06).
 */
@Configuration
public class TimeConfig {

	public static final ZoneId BALLPARK_ZONE = ZoneId.of("Asia/Seoul");

	@Bean
	public Clock clock() {
		return Clock.system(BALLPARK_ZONE);
	}
}
