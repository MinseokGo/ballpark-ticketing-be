package com.ballpark.ticketing.common.auth;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.user.JwtService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** JWT 서비스는 필요할 때 꺼낸다. 웹 설정이 이 객체를 만들 때 빈이 없어도 실패하지 않게 하려는 것이다. */
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String PREFIX = "Bearer ";

	private final ObjectProvider<JwtService> jwtService;

	public LoginUserArgumentResolver(ObjectProvider<JwtService> jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(LoginUser.class) && Long.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		String header = webRequest.getHeader("Authorization");
		if (header == null || !header.startsWith(PREFIX)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return jwtService.getObject().parse(header.substring(PREFIX.length()).trim())
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}
}
