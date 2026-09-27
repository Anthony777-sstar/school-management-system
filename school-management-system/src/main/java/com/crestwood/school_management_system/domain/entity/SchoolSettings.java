package com.crestwood.school_management_system.domain.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_settings")
public class SchoolSettings {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "school_name", nullable = false, length = 200)
	private String schoolName;

	@Column(name = "logo_url", length = 500)
	private String logoUrl;

	@Column(name = "primary_color", nullable = false, length = 20)
	private String primaryColor;

	@CreationTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SchoolSettings() {
	}

	public SchoolSettings(String schoolName, String logoUrl, String primaryColor) {
		this.schoolName = schoolName;
		this.logoUrl = logoUrl;
		this.primaryColor = primaryColor;
	}

	public Long getId() {
		return id;
	}

	public String getSchoolName() {
		return schoolName;
	}

	public void setSchoolName(String schoolName) {
		this.schoolName = schoolName;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public String getPrimaryColor() {
		return primaryColor;
	}

	public void setPrimaryColor(String primaryColor) {
		this.primaryColor = primaryColor;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
