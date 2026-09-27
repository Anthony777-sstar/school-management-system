package com.crestwood.school_management_system.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.Notification;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.NotificationType;
import com.crestwood.school_management_system.dto.ApiDtos.NotificationResponse;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.NotificationRepository;

@Service
public class NotificationService {
	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Transactional
	public Notification create(User recipient, NotificationType type, String title, String message) {
		if (recipient == null || !recipient.isEnabled()) {
			throw new IllegalArgumentException("Notification recipient is invalid");
		}
		return notificationRepository.save(new Notification(recipient, type, title, message));
	}

	@Transactional
	public List<Notification> createAll(Collection<User> recipients, NotificationType type, String title, String message) {
		return recipients.stream().filter(User::isEnabled).distinct().map(recipient -> notificationRepository.save(new Notification(recipient, type, title, message))).toList();
	}

	@Transactional(readOnly = true)
	public List<NotificationResponse> list(User user) {
		return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::response).toList();
	}

	@Transactional
	public NotificationResponse markRead(User user, Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new NotFoundException("Notification not found"));
		if (!notification.getRecipientUser().getId().equals(user.getId())) {
			throw new ForbiddenException("Notification is not addressed to you");
		}
		notification.setRead(true);
		return response(notificationRepository.save(notification));
	}

	@Transactional(readOnly = true)
	public long unreadCount(User user) {
		return notificationRepository.countByRecipientUserIdAndReadFalse(user.getId());
	}

	private NotificationResponse response(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(), notification.getMessage(), notification.isRead(), notification.getCreatedAt());
	}
}
