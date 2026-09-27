package com.crestwood.school_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
	long countByRecipientUserIdAndReadFalse(Long recipientUserId);
}
