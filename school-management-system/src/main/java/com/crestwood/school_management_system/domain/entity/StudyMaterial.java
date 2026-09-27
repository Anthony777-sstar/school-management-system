package com.crestwood.school_management_system.domain.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_materials")
public class StudyMaterial {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private Teacher teacher;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "school_class_id", nullable = false)
	private SchoolClass schoolClass;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "file_url", nullable = false, length = 500)
	private String fileUrl;

	@Column(name = "stored_filename", nullable = false, unique = true, length = 255)
	private String storedFilename;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "content_type", nullable = false, length = 120)
	private String contentType;

	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	@CreationTimestamp
	@Column(name = "uploaded_at", nullable = false, updatable = false)
	private Instant uploadedAt;

	protected StudyMaterial() {
	}

	public StudyMaterial(Teacher teacher, Subject subject, SchoolClass schoolClass, String title, String storedFilename, String originalFilename, String contentType, Long fileSize) {
		this.teacher = teacher;
		this.subject = subject;
		this.schoolClass = schoolClass;
		this.title = title;
		this.fileUrl = "";
		this.storedFilename = storedFilename;
		this.originalFilename = originalFilename;
		this.contentType = contentType;
		this.fileSize = fileSize;
	}

	public Long getId() {
		return id;
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public Subject getSubject() {
		return subject;
	}

	public SchoolClass getSchoolClass() {
		return schoolClass;
	}

	public String getTitle() {
		return title;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public String getStoredFilename() {
		return storedFilename;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public String getContentType() {
		return contentType;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public Instant getUploadedAt() {
		return uploadedAt;
	}
}
