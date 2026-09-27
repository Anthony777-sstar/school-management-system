package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.crestwood.school_management_system.domain.entity.FeeInvoice;

import jakarta.persistence.LockModeType;

public interface FeeInvoiceRepository extends JpaRepository<FeeInvoice, Long> {
	Optional<FeeInvoice> findBySchoolClassIdAndTermId(Long schoolClassId, Long termId);
	List<FeeInvoice> findByTermId(Long termId);
	List<FeeInvoice> findBySchoolClassId(Long schoolClassId);
	List<FeeInvoice> findAllByOrderByCreatedAtDesc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from FeeInvoice i where i.id = :id")
	Optional<FeeInvoice> findByIdForUpdate(@Param("id") Long id);
}
