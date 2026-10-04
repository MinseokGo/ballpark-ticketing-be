package com.ballpark.ticketing.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ballpark-ticketing-fe(별도 레포, 로컬 Vite 개발 서버)에서 API를 호출할 수 있도록 CORS를 허용한다.
 * 운영 오리진은 아직 없어 app.cors.allowed-origins로 환경별로 바꿀 수 있게만 둔다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.cors.allowed-origins:http://localhost:5173}")
	private String[] allowedOrigins;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
	}
}
