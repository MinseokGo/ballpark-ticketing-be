package com.ballpark.ticketing.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.user.dto.AuthResponse;
import com.ballpark.ticketing.user.dto.LoginRequest;
import com.ballpark.ticketing.user.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void signupIssuesAUsableTokenAndStoresAHashedPassword() {
		AuthResponse response = authService.signup(new SignupRequest("Fan@Example.com", "password123", "가윤"));

		assertThat(response.user().email()).isEqualTo("fan@example.com");
		assertThat(jwtService.parse(response.token())).contains(response.user().id());
		assertThat(userRepository.findById(response.user().id()).orElseThrow().getPasswordHash())
				.isNotEqualTo("password123");
	}

	@Test
	void loginWorksWithTheRightPasswordOnly() {
		authService.signup(new SignupRequest("login@example.com", "password123", "서준"));

		AuthResponse ok = authService.login(new LoginRequest("login@example.com", "password123"));

		assertThat(ok.user().nickname()).isEqualTo("서준");
		assertThatThrownBy(() -> authService.login(new LoginRequest("login@example.com", "wrong-password")))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.LOGIN_FAILED);
	}

	@Test
	void rejectsADuplicateEmail() {
		authService.signup(new SignupRequest("dup@example.com", "password123", "지호"));

		assertThatThrownBy(() -> authService.signup(new SignupRequest("DUP@example.com", "password123", "하린")))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.EMAIL_DUPLICATE);
	}

	@Test
	void aTamperedTokenIsRejected() {
		AuthResponse response = authService.signup(new SignupRequest("tamper@example.com", "password123", "수아"));

		assertThat(jwtService.parse(response.token() + "x")).isEmpty();
	}
}
