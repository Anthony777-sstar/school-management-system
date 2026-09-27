package com.crestwood.school_management_system.domain.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.crestwood.school_management_system.domain.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_user_id", nullable = false)
	private User recipientUser;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private NotificationType type;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "message", nullable = false, length = 2000)
	private String message;

	@Column(name = "is_read", nullable = false)
	private boolean read;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Notification() {
	}

	public Notification(User recipientUser, NotificationType type, String title, String message) {
		this.recipientUser = recipientUser;
		this.type = type;
		this.title = title;
		this.message = message;
		this.read = false;
	}

	public Long getId() {
		return id;
	}

	public User getRecipientUser() {
		return recipientUser;
	}

	public NotificationType getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
