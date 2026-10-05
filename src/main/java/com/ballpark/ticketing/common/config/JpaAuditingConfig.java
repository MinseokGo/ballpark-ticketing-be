package com.ballpark.ticketing.common.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// 감사 시각은 Clock에서 읽으므로 TimeConfig를 같이 올린다. 슬라이스 테스트가 이 설정만 import해도 빈이 빠지지 않는다.
@Configuration
@EnableJpaAuditing
@Import(TimeConfig.class)
public class JpaAuditingConfig {

	// createdAt도 같은 Clock을 쓴다. 기본값은 JVM 기본 시간대를 따르므로 서버 설정에 따라 달라진다.
	@Bean
	public DateTimeProvider auditingDateTimeProvider(Clock clock) {
		return () -> Optional.of(LocalDateTime.now(clock));
	}
}
