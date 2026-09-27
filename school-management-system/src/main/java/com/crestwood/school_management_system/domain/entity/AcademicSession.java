package com.crestwood.school_management_system.domain.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "academic_sessions", uniqueConstraints = @UniqueConstraint(name = "uk_academic_sessions_name", columnNames = "name"))
public class AcademicSession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 30)
	private String name;

	@Column(name = "current", nullable = false)
	private boolean current;

	@OneToMany(mappedBy = "academicSession", fetch = FetchType.LAZY)
	private List<Term> terms = new ArrayList<>();

	protected AcademicSession() {
	}

	public AcademicSession(String name, boolean current) {
		this.name = name;
		this.current = current;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}

	public List<Term> getTerms() {
		return terms;
	}
}
