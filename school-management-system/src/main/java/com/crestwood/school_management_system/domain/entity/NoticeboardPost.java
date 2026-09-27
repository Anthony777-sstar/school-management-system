package com.crestwood.school_management_system.domain.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.crestwood.school_management_system.domain.enums.NoticeboardAudience;

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
@Table(name = "noticeboard_posts")
public class NoticeboardPost {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "body", nullable = false, length = 5000)
	private String body;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "posted_by_user_id", nullable = false)
	private User postedByUser;

	@Enumerated(EnumType.STRING)
	@Column(name = "audience", nullable = false, length = 30)
	private NoticeboardAudience audience;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "audience_class_id")
	private SchoolClass audienceClass;

	@CreationTimestamp
	@Column(name = "posted_at", nullable = false, updatable = false)
	private Instant postedAt;

	protected NoticeboardPost() {
	}

	public NoticeboardPost(String title, String body, User postedByUser, NoticeboardAudience audience, SchoolClass audienceClass) {
		this.title = title;
		this.body = body;
		this.postedByUser = postedByUser;
		this.audience = audience;
		this.audienceClass = audienceClass;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public User getPostedByUser() {
		return postedByUser;
	}

	public NoticeboardAudience getAudience() {
		return audience;
	}

	public SchoolClass getAudienceClass() {
		return audienceClass;
	}

	public Instant getPostedAt() {
		return postedAt;
	}
}
