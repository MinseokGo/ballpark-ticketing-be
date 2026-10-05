package com.ballpark.ticketing.game.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 중계 생성기 설정. enabled는 local 프로필에서만 켠다. speed는 배속이며 1.0이 실제 야구 페이스다.
 */
@ConfigurationProperties(prefix = "app.live-simulator")
public record LiveSimulatorProperties(boolean enabled, @DefaultValue("1.0") double speed) {
}
