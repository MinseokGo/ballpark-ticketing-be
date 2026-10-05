package com.ballpark.ticketing.common.config;

import com.ballpark.ticketing.common.auth.LoginUserArgumentResolver;
import com.ballpark.ticketing.user.JwtService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ballpark-ticketing-fe(별도 레포, 로컬 Vite 개발 서버)에서 API를 호출할 수 있도록 CORS를 허용한다.
 * 운영 오리진은 아직 없어 app.cors.allowed-origins로 환경별로 바꿀 수 있게만 둔다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.cors.allowed-origins:http://localhost:5173}")
	private String[] allowedOrigins;

	private final ObjectProvider<JwtService> jwtService;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				.allowedHeaders("Authorization", "Content-Type", "Last-Event-ID")
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new LoginUserArgumentResolver(jwtService));
	}
}
