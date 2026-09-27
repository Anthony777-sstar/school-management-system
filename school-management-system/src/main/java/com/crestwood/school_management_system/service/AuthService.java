package com.crestwood.school_management_system.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.PasswordResetToken;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.dto.ApiDtos.ChangePasswordRequest;
import com.crestwood.school_management_system.dto.ApiDtos.ForgotPasswordRequest;
import com.crestwood.school_management_system.dto.ApiDtos.LoginRequest;
import com.crestwood.school_management_system.dto.ApiDtos.LoginResponse;
import com.crestwood.school_management_system.dto.ApiDtos.ResetPasswordRequest;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ServiceUnavailableException;
import com.crestwood.school_management_system.exception.UnauthorizedException;
import com.crestwood.school_management_system.repository.PasswordResetTokenRepository;
import com.crestwood.school_management_system.repository.UserRepository;
import com.crestwood.school_management_system.security.JwtService;

@Service
public class AuthService {
	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
	private static final String GENERIC_RESET_MESSAGE = "If that email exists in our system, a reset link was sent";
	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final UserService userService;
	private final MailService mailService;
	private final CredentialService credentialService;

	public AuthService(
			AuthenticationManager authenticationManager,
			UserRepository userRepository,
			PasswordResetTokenRepository tokenRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			UserService userService,
			MailService mailService,
			CredentialService credentialService) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.userService = userService;
		this.mailService = mailService;
		this.credentialService = credentialService;
	}

	public LoginResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase();
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
		} catch (BadCredentialsException | DisabledException exception) {
			throw new UnauthorizedException("Invalid email or password");
		}
		User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
		if (request.role() != null && request.role() != user.getRole()) {
			throw new UnauthorizedException("Invalid email or password");
		}
		String token = jwtService.issue(user);
		return new LoginResponse(token, "Bearer", jwtService.getExpirationMs() / 1000, userService.response(user));
	}

	@Transactional
	public void forgotPassword(ForgotPasswordRequest request) {
		User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
		if (user == null) {
			return;
		}
		tokenRepository.findByUserIdAndUsedFalse(user.getId()).forEach(token -> token.setUsed(true));
		PasswordResetToken resetToken = tokenRepository.save(new PasswordResetToken(user, credentialService.createResetToken(), Instant.now().plus(30, ChronoUnit.MINUTES)));
		try {
			mailService.sendPasswordReset(user.getEmail(), resetToken.getToken());
		} catch (ServiceUnavailableException exception) {
			logger.error("Password reset email delivery failed");
		}
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		PasswordResetToken resetToken = tokenRepository.findByTokenForUpdate(request.token()).orElseThrow(() -> new BadRequestException("Password reset token is invalid"));
		if (resetToken.isUsed() || !resetToken.getExpiresAt().isAfter(Instant.now())) {
			throw new BadRequestException("Password reset token is invalid or expired");
		}
		User user = resetToken.getUser();
		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
		tokenRepository.findByUserIdAndUsedFalse(user.getId()).forEach(token -> token.setUsed(true));
	}

	@Transactional
	public void changePassword(User user, ChangePasswordRequest request) {
		User managedUser = userRepository.findById(user.getId()).orElseThrow(() -> new UnauthorizedException("Authentication is required"));
		if (!passwordEncoder.matches(request.currentPassword(), managedUser.getPassword())) {
			throw new BadRequestException("Current password is incorrect");
		}
		managedUser.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(managedUser);
		tokenRepository.findByUserIdAndUsedFalse(managedUser.getId()).forEach(token -> token.setUsed(true));
	}

	public String genericResetMessage() {
		return GENERIC_RESET_MESSAGE;
	}
}
