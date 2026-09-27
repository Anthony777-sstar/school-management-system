package com.crestwood.school_management_system.domain.entity;

import java.time.LocalDate;

import com.crestwood.school_management_system.domain.enums.TermName;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "terms", uniqueConstraints = @UniqueConstraint(name = "uk_terms_session_name", columnNames = { "academic_session_id", "name" }))
public class Term {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_session_id", nullable = false)
	private AcademicSession academicSession;

	@Enumerated(EnumType.STRING)
	@Column(name = "name", nullable = false, length = 20)
	private TermName name;

	@Column(name = "current", nullable = false)
	private boolean current;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	protected Term() {
	}

	public Term(AcademicSession academicSession, TermName name, boolean current, LocalDate startDate, LocalDate endDate) {
		this.academicSession = academicSession;
		this.name = name;
		this.current = current;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public Long getId() {
		return id;
	}

	public AcademicSession getAcademicSession() {
		return academicSession;
	}

	public TermName getName() {
		return name;
	}

	public void setName(TermName name) {
		this.name = name;
	}

	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}
}
