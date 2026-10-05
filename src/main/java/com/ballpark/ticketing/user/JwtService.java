package com.ballpark.ticketing.user;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 로그인 토큰(JWT, HS256). 토큰의 subject가 사용자 번호다. */
@Service
public class JwtService {

	private final SecretKey key;
	private final long expiryMinutes;

	public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiry-minutes}") long expiryMinutes) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expiryMinutes = expiryMinutes;
	}

	public String issue(Long userId) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expiryMinutes, ChronoUnit.MINUTES)))
				.signWith(key)
				.compact();
	}

	/** 서명과 만료가 맞으면 사용자 번호를 돌려준다. 아니면 빈 값. */
	public Optional<Long> parse(String token) {
		try {
			String subject = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
			return Optional.of(Long.valueOf(subject));
		} catch (JwtException | IllegalArgumentException error) {
			return Optional.empty();
		}
	}
}
