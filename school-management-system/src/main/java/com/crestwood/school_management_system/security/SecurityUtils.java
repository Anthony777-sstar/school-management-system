package com.crestwood.school_management_system.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.exception.ForbiddenException;

public final class SecurityUtils {
	private SecurityUtils() {
	}

	public static UserPrincipal principal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
			throw new ForbiddenException("Authentication is required");
		}
		return principal;
	}

	public static User user() {
		return principal().getUser();
	}
}
