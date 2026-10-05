package com.ballpark.ticketing.user;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.user.dto.AuthResponse;
import com.ballpark.ticketing.user.dto.LoginRequest;
import com.ballpark.ticketing.user.dto.SignupRequest;
import com.ballpark.ticketing.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public AuthResponse signup(SignupRequest request) {
		String email = request.email().trim().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
		}
		AppUser user = userRepository.save(
				new AppUser(email, passwordEncoder.encode(request.password()), request.nickname().trim()));
		return new AuthResponse(jwtService.issue(user.getId()), UserResponse.from(user));
	}

	/** 이메일이 없거나 비밀번호가 틀리면 같은 오류를 준다(어느 쪽이 틀렸는지 알려 주지 않는다). */
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		AppUser user = userRepository.findByEmail(request.email().trim().toLowerCase())
				.filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
				.orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
		return new AuthResponse(jwtService.issue(user.getId()), UserResponse.from(user));
	}

	@Transactional(readOnly = true)
	public UserResponse me(Long userId) {
		return userRepository.findById(userId)
				.map(UserResponse::from)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}
}
