package com.crestwood.school_management_system.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class CredentialService {
	private final SecureRandom secureRandom = new SecureRandom();

	public String createTemporaryPassword() {
		byte[] bytes = new byte[15];
		secureRandom.nextBytes(bytes);
		return "CWA-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public String createResetToken() {
		byte[] bytes = new byte[48];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
