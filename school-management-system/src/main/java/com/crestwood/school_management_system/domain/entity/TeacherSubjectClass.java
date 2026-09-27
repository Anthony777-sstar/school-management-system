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
@Table(name = "teacher_subject_classes", uniqueConstraints = @UniqueConstraint(name = "uk_teacher_subject_class", columnNames = { "teacher_id", "subject_id", "school_class_id" }))
public class TeacherSubjectClass {
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

	protected TeacherSubjectClass() {
	}

	public TeacherSubjectClass(Teacher teacher, Subject subject, SchoolClass schoolClass) {
		this.teacher = teacher;
		this.subject = subject;
		this.schoolClass = schoolClass;
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
}
