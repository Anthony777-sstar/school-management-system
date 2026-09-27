package com.crestwood.school_management_system.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.crestwood.school_management_system.domain.entity.FeePayment;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {
	List<FeePayment> findByStudentIdOrderByCreatedAtAscIdAsc(Long studentId);
	List<FeePayment> findByFeeInvoiceIdOrderByIdAsc(Long feeInvoiceId);
	List<FeePayment> findAllByOrderByIdDesc();
	List<FeePayment> findByStatusOrderByIdDesc(PaymentStatus status);
	Optional<FeePayment> findByPaystackReference(String paystackReference);
	List<FeePayment> findByFeeInvoiceIdAndStatus(Long feeInvoiceId, PaymentStatus status);
	List<FeePayment> findByStudentIdAndFeeInvoiceIdAndStatus(Long studentId, Long feeInvoiceId, PaymentStatus status);
	boolean existsByStudentIdAndFeeInvoiceIdAndStatusAndPaymentMethod(Long studentId, Long feeInvoiceId, PaymentStatus status, com.crestwood.school_management_system.domain.enums.PaymentMethod paymentMethod);
	List<FeePayment> findByStatusAndPaidAtBetweenOrderByPaidAtDesc(PaymentStatus status, Instant from, Instant to);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from FeePayment p where p.id = :id")
	Optional<FeePayment> findByIdForUpdate(@Param("id") Long id);
}
