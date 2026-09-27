package com.crestwood.school_management_system.domain.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
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
@Table(name = "results", uniqueConstraints = @UniqueConstraint(name = "uk_result_student_subject_term", columnNames = { "student_id", "subject_id", "term_id" }))
public class Result {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "term_id", nullable = false)
	private Term term;

	@Column(name = "ca1", nullable = false)
	private int ca1;

	@Column(name = "ca2", nullable = false)
	private int ca2;

	@Column(name = "exam", nullable = false)
	private int exam;

	@Column(name = "total", nullable = false)
	private int total;

	@Column(name = "grade", nullable = false, length = 2)
	private String grade;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "entered_by_teacher_id", nullable = false)
	private Teacher enteredByTeacher;

	@CreationTimestamp
	@Column(name = "entered_at", nullable = false, updatable = false)
	private Instant enteredAt;

	protected Result() {
	}

	public Result(Student student, Subject subject, Term term, int ca1, int ca2, int exam, int total, String grade, Teacher enteredByTeacher) {
		this.student = student;
		this.subject = subject;
		this.term = term;
		this.ca1 = ca1;
		this.ca2 = ca2;
		this.exam = exam;
		this.total = total;
		this.grade = grade;
		this.enteredByTeacher = enteredByTeacher;
	}

	public Long getId() {
		return id;
	}

	public Student getStudent() {
		return student;
	}

	public Subject getSubject() {
		return subject;
	}

	public Term getTerm() {
		return term;
	}

	public int getCa1() {
		return ca1;
	}

	public int getCa2() {
		return ca2;
	}

	public int getExam() {
		return exam;
	}

	public int getTotal() {
		return total;
	}

	public String getGrade() {
		return grade;
	}

	public Teacher getEnteredByTeacher() {
		return enteredByTeacher;
	}

	public Instant getEnteredAt() {
		return enteredAt;
	}

	public void updateScores(int ca1, int ca2, int exam, int total, String grade, Teacher enteredByTeacher) {
		this.ca1 = ca1;
		this.ca2 = ca2;
		this.exam = exam;
		this.total = total;
		this.grade = grade;
		this.enteredByTeacher = enteredByTeacher;
	}
}
