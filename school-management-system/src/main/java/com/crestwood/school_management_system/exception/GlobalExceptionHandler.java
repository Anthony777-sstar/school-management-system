package com.crestwood.school_management_system.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, String>> handleApiException(ApiException exception) {
		String message = safeMessage(exception.getMessage(), "The request could not be completed");
		return ResponseEntity.status(exception.getStatus()).body(Map.of("error", message));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
		return validation(exception);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<Map<String, String>> handleBindException(BindException exception) {
		return validation(exception);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException exception) {
		var violation = exception.getConstraintViolations().stream().findFirst().orElse(null);
		String message = violation == null || violation.getMessage() == null ? "Request validation failed" : violation.getMessage();
		return ResponseEntity.badRequest().body(Map.of("error", message));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<Map<String, String>> handleMethodValidation(HandlerMethodValidationException exception) {
		String message = exception.getAllErrors().stream()
				.map(error -> error.getDefaultMessage())
				.filter(value -> value != null && !value.isBlank())
				.findFirst()
				.orElse("Request validation failed");
		return ResponseEntity.badRequest().body(Map.of("error", message));
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class, MissingServletRequestPartException.class, IllegalArgumentException.class})
	public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
		return ResponseEntity.badRequest().body(Map.of("error", "Request body or parameters are invalid"));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleUploadSize(MaxUploadSizeExceededException exception) {
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(Map.of("error", "The uploaded file exceeds the configured size limit"));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to perform this action"));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication is required"));
	}

	@ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
	public ResponseEntity<Map<String, String>> handleDataConflict(Exception exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "The requested record conflicts with existing data"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(NoResourceFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "The requested resource was not found"));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("error", "HTTP method is not supported for this resource"));
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<Map<String, String>> handleMediaType(HttpMediaTypeNotSupportedException exception) {
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of("error", "Content type is not supported for this resource"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
		logger.error("Unhandled API exception", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected server error occurred"));
	}

	private ResponseEntity<Map<String, String>> validation(BindException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		String message = errors.values().stream().findFirst().orElse("Request validation failed");
		return ResponseEntity.badRequest().body(Map.of("error", safeMessage(message, "Request validation failed")));
	}

	private String safeMessage(String message, String fallback) {
		return message == null || message.isBlank() ? fallback : message;
	}
}
