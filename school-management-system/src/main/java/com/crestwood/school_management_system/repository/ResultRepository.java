package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.Result;

public interface ResultRepository extends JpaRepository<Result, Long> {
	List<Result> findByStudentIdOrderByIdAsc(Long studentId);
	List<Result> findByStudentIdAndTermIdOrderBySubjectIdAsc(Long studentId, Long termId);
	Optional<Result> findByStudentIdAndSubjectIdAndTermId(Long studentId, Long subjectId, Long termId);
	List<Result> findByTermId(Long termId);
}
