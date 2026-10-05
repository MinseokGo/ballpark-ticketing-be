package com.ballpark.ticketing.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// SSE heartbeat(GameLiveEventHub.heartbeat)가 돌도록 켠다.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
