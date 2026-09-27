package com.crestwood.school_management_system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.NotificationResponse;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public List<NotificationResponse> list() {
		return notificationService.list(SecurityUtils.user());
	}

	@GetMapping("/unread-count")
	public Map<String, Long> unreadCount() {
		return Map.of("count", notificationService.unreadCount(SecurityUtils.user()));
	}

	@PatchMapping("/{id}/read")
	public NotificationResponse markRead(@PathVariable Long id) {
		return notificationService.markRead(SecurityUtils.user(), id);
	}
}
