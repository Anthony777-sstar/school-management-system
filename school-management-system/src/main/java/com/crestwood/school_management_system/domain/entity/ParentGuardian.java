package com.crestwood.school_management_system.domain.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "parent_guardians")
public class ParentGuardian {
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

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	private List<StudentParentLink> studentLinks = new ArrayList<>();

	protected ParentGuardian() {
	}

	public ParentGuardian(User user, String fullName, String phone) {
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

	public List<StudentParentLink> getStudentLinks() {
		return studentLinks;
	}
}
