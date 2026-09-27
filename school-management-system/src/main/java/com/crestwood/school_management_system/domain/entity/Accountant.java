package com.crestwood.school_management_system.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "accountants")
public class Accountant {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "full_name", nullable = false, length = 160)
	private String fullName;

	@Column(name = "phone", nullable = false, length = 40)
	private String phone;

	protected Accountant() {
	}

	public Accountant(User user, String fullName, String phone) {
		this.user = user;
		this.fullName = fullName;
		this.phone = phone;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
}
