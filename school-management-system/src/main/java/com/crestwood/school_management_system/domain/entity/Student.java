package com.crestwood.school_management_system.domain.entity;

import java.time.LocalDate;

import com.crestwood.school_management_system.domain.enums.RosterFlag;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "full_name", nullable = false, length = 160)
	private String fullName;

	@Column(name = "date_of_birth", nullable = false)
	private LocalDate dateOfBirth;

	@Column(name = "gender", nullable = false, length = 20)
	private String gender;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "school_class_id", nullable = false)
	private SchoolClass schoolClass;

	@Column(name = "roster_confirmed", nullable = false)
	private boolean rosterConfirmed;

	@Enumerated(EnumType.STRING)
	@Column(name = "roster_flag", nullable = false, length = 20)
	private RosterFlag rosterFlag;

	@Column(name = "admission_date", nullable = false)
	private LocalDate admissionDate;

	protected Student() {
	}

	public Student(User user, String fullName, LocalDate dateOfBirth, String gender, SchoolClass schoolClass, LocalDate admissionDate) {
		this.user = user;
		this.fullName = fullName;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.schoolClass = schoolClass;
		this.admissionDate = admissionDate;
		this.rosterConfirmed = false;
		this.rosterFlag = RosterFlag.NONE;
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

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public SchoolClass getSchoolClass() {
		return schoolClass;
	}

	public void setSchoolClass(SchoolClass schoolClass) {
		this.schoolClass = schoolClass;
	}

	public boolean isRosterConfirmed() {
		return rosterConfirmed;
	}

	public void setRosterConfirmed(boolean rosterConfirmed) {
		this.rosterConfirmed = rosterConfirmed;
	}

	public RosterFlag getRosterFlag() {
		return rosterFlag;
	}

	public void setRosterFlag(RosterFlag rosterFlag) {
		this.rosterFlag = rosterFlag;
	}

	public LocalDate getAdmissionDate() {
		return admissionDate;
	}

	public void setAdmissionDate(LocalDate admissionDate) {
		this.admissionDate = admissionDate;
	}

	public void confirmRoster() {
		this.rosterConfirmed = true;
		this.rosterFlag = RosterFlag.NONE;
	}

	public void flagRoster() {
		this.rosterConfirmed = false;
		this.rosterFlag = RosterFlag.FLAGGED;
	}

	public void clearRoster() {
		this.rosterConfirmed = false;
		this.rosterFlag = RosterFlag.NONE;
	}
}
