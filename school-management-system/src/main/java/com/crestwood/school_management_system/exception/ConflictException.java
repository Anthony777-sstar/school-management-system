package com.crestwood.school_management_system.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
	public ConflictException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
