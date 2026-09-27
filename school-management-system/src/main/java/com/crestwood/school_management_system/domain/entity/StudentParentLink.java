package com.crestwood.school_management_system.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "student_parent_links", uniqueConstraints = @UniqueConstraint(name = "uk_student_parent_link", columnNames = { "parent_id", "student_id" }))
public class StudentParentLink {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parent_id", nullable = false)
	private ParentGuardian parent;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	protected StudentParentLink() {
	}

	public StudentParentLink(ParentGuardian parent, Student student) {
		this.parent = parent;
		this.student = student;
	}

	public Long getId() {
		return id;
	}

	public ParentGuardian getParent() {
		return parent;
	}

	public Student getStudent() {
		return student;
	}
}
