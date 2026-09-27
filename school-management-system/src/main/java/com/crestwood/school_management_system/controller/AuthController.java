package com.crestwood.school_management_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.ChangePasswordRequest;
import com.crestwood.school_management_system.dto.ApiDtos.ForgotPasswordRequest;
import com.crestwood.school_management_system.dto.ApiDtos.LoginRequest;
import com.crestwood.school_management_system.dto.ApiDtos.LoginResponse;
import com.crestwood.school_management_system.dto.ApiDtos.MessageResponse;
import com.crestwood.school_management_system.dto.ApiDtos.ResetPasswordRequest;
import com.crestwood.school_management_system.dto.ApiDtos.UserResponse;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.AuthService;
import com.crestwood.school_management_system.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final AuthService authService;
	private final UserService userService;

	public AuthController(AuthService authService, UserService userService) {
		this.authService = authService;
		this.userService = userService;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/forgot-password")
	public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request);
		return new MessageResponse(authService.genericResetMessage());
	}

	@PostMapping("/reset-password")
	public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return new MessageResponse("Your password was updated");
	}

	@GetMapping("/me")
	public UserResponse me() {
		return userService.response(SecurityUtils.user());
	}

	@PutMapping("/password")
	public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(SecurityUtils.user(), request);
		return new MessageResponse("Your password was updated");
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent().build();
	}
}
