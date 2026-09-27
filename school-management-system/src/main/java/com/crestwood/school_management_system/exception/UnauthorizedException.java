package com.crestwood.school_management_system.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {
	public UnauthorizedException(String message) {
		super(HttpStatus.UNAUTHORIZED, message);
	}
}
