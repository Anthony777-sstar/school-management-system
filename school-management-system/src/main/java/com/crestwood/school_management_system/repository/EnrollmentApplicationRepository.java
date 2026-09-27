package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.EnrollmentApplication;
import com.crestwood.school_management_system.domain.enums.EnrollmentStatus;

public interface EnrollmentApplicationRepository extends JpaRepository<EnrollmentApplication, Long> {
	List<EnrollmentApplication> findByStatusOrderBySubmittedAtDesc(EnrollmentStatus status);
	List<EnrollmentApplication> findAllByOrderBySubmittedAtDesc();
	long countByStatus(EnrollmentStatus status);
	Optional<EnrollmentApplication> findFirstByParentEmailIgnoreCaseAndStudentFullNameIgnoreCaseAndApplyingForClassIdAndStatus(String parentEmail, String studentFullName, Long applyingForClassId, EnrollmentStatus status);
}
