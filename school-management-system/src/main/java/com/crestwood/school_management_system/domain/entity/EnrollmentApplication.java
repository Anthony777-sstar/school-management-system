package com.crestwood.school_management_system.domain.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.crestwood.school_management_system.domain.enums.EnrollmentStatus;

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
@Table(name = "enrollment_applications")
public class EnrollmentApplication {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "parent_full_name", nullable = false, length = 160)
	private String parentFullName;

	@Column(name = "parent_email", nullable = false, length = 320)
	private String parentEmail;

	@Column(name = "parent_phone", nullable = false, length = 40)
	private String parentPhone;

	@Column(name = "student_full_name", nullable = false, length = 160)
	private String studentFullName;

	@Column(name = "student_date_of_birth", nullable = false)
	private LocalDate studentDateOfBirth;

	@Column(name = "student_gender", nullable = false, length = 20)
	private String studentGender;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "applying_for_class_id", nullable = false)
	private SchoolClass applyingForClass;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private EnrollmentStatus status;

	@CreationTimestamp
	@Column(name = "submitted_at", nullable = false, updatable = false)
	private Instant submittedAt;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by_user_id")
	private User reviewedByUser;

	protected EnrollmentApplication() {
	}

	public EnrollmentApplication(String parentFullName, String parentEmail, String parentPhone, String studentFullName, LocalDate studentDateOfBirth, String studentGender, SchoolClass applyingForClass) {
		this.parentFullName = parentFullName;
		this.parentEmail = parentEmail;
		this.parentPhone = parentPhone;
		this.studentFullName = studentFullName;
		this.studentDateOfBirth = studentDateOfBirth;
		this.studentGender = studentGender == null || studentGender.isBlank() ? "NOT_SPECIFIED" : studentGender.trim().toUpperCase();
		this.applyingForClass = applyingForClass;
		this.status = EnrollmentStatus.PENDING;
	}

	public Long getId() {
		return id;
	}

	public String getParentFullName() {
		return parentFullName;
	}

	public String getParentEmail() {
		return parentEmail;
	}

	public String getParentPhone() {
		return parentPhone;
	}

	public String getStudentFullName() {
		return studentFullName;
	}

	public LocalDate getStudentDateOfBirth() {
		return studentDateOfBirth;
	}

	public String getStudentGender() {
		return studentGender;
	}

	public SchoolClass getApplyingForClass() {
		return applyingForClass;
	}

	public EnrollmentStatus getStatus() {
		return status;
	}

	public void setStatus(EnrollmentStatus status) {
		this.status = status;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(Instant reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public User getReviewedByUser() {
		return reviewedByUser;
	}

	public void setReviewedByUser(User reviewedByUser) {
		this.reviewedByUser = reviewedByUser;
	}
}
