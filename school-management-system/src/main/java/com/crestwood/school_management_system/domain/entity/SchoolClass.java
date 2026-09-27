package com.crestwood.school_management_system.domain.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "school_classes", uniqueConstraints = @UniqueConstraint(name = "uk_school_classes_name", columnNames = "name"))
public class SchoolClass {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@jakarta.persistence.Column(name = "name", nullable = false, length = 80)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class_teacher_id")
	private Teacher classTeacher;

	@OneToMany(mappedBy = "schoolClass", fetch = FetchType.LAZY)
	private List<Student> students = new ArrayList<>();

	protected SchoolClass() {
	}

	public SchoolClass(String name) {
		this.name = name;
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

	public Teacher getClassTeacher() {
		return classTeacher;
	}

	public void setClassTeacher(Teacher classTeacher) {
		this.classTeacher = classTeacher;
	}

	public List<Student> getStudents() {
		return students;
	}
}
