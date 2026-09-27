package com.crestwood.school_management_system.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.exception.ServiceUnavailableException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	private final String secret;
	private final long expirationMs;

	public JwtService(@Value("${app.jwt.secret:}") String secret, @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {
		this.secret = secret;
		this.expirationMs = expirationMs;
	}

	public String issue(User user) {
		requireConfigured();
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMs)))
				.signWith(key())
				.compact();
	}

	public Claims parse(String token) {
		requireConfigured();
		Jws<Claims> parsed = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
		return parsed.getPayload();
	}

	public long getExpirationMs() {
		return expirationMs;
	}

	private SecretKey key() {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new ServiceUnavailableException("JWT service is unavailable: JWT_SECRET must be at least 32 characters");
		}
		return Keys.hmacShaKeyFor(bytes);
	}

	private void requireConfigured() {
		if (secret == null || secret.isBlank()) {
			throw new ServiceUnavailableException("JWT service is unavailable: JWT_SECRET is not configured");
		}
	}
}
