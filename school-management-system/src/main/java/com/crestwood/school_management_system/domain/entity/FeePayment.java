package com.crestwood.school_management_system.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.crestwood.school_management_system.domain.enums.PaymentMethod;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;

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
@Table(name = "fee_payments", uniqueConstraints = @UniqueConstraint(name = "uk_fee_payment_paystack_reference", columnNames = "paystack_reference"))
public class FeePayment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fee_invoice_id", nullable = false)
	private FeeInvoice feeInvoice;

	@Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
	private BigDecimal amountPaid;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 20)
	private PaymentMethod paymentMethod;

	@Column(name = "paystack_reference", length = 120)
	private String paystackReference;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "paid_at")
	private Instant paidAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recorded_by_accountant_id")
	private Accountant recordedByAccountant;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected FeePayment() {
	}

	public FeePayment(Student student, FeeInvoice feeInvoice, BigDecimal amountPaid, PaymentMethod paymentMethod, String paystackReference, PaymentStatus status, Accountant recordedByAccountant) {
		this.student = student;
		this.feeInvoice = feeInvoice;
		this.amountPaid = amountPaid;
		this.paymentMethod = paymentMethod;
		this.paystackReference = paystackReference;
		this.status = status;
		this.recordedByAccountant = recordedByAccountant;
	}

	public Long getId() {
		return id;
	}

	public Student getStudent() {
		return student;
	}

	public FeeInvoice getFeeInvoice() {
		return feeInvoice;
	}

	public BigDecimal getAmountPaid() {
		return amountPaid;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public String getPaystackReference() {
		return paystackReference;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
	}

	public Instant getPaidAt() {
		return paidAt;
	}

	public void setPaidAt(Instant paidAt) {
		this.paidAt = paidAt;
	}

	public Accountant getRecordedByAccountant() {
		return recordedByAccountant;
	}

	public void setRecordedByAccountant(Accountant recordedByAccountant) {
		this.recordedByAccountant = recordedByAccountant;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
