package com.crestwood.school_management_system.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "fee_invoices", uniqueConstraints = @UniqueConstraint(name = "uk_fee_invoice_class_term", columnNames = { "school_class_id", "term_id" }))
public class FeeInvoice {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "school_class_id", nullable = false)
	private SchoolClass schoolClass;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "term_id", nullable = false)
	private Term term;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_accountant_id", nullable = false)
	private Accountant createdByAccountant;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected FeeInvoice() {
	}

	public FeeInvoice(SchoolClass schoolClass, Term term, BigDecimal amount, LocalDate dueDate, Accountant createdByAccountant) {
		this.schoolClass = schoolClass;
		this.term = term;
		this.amount = amount;
		this.dueDate = dueDate;
		this.createdByAccountant = createdByAccountant;
	}

	public Long getId() {
		return id;
	}

	public SchoolClass getSchoolClass() {
		return schoolClass;
	}

	public Term getTerm() {
		return term;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public Accountant getCreatedByAccountant() {
		return createdByAccountant;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
