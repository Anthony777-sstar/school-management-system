package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.Term;
import com.crestwood.school_management_system.domain.enums.TermName;

public interface TermRepository extends JpaRepository<Term, Long> {
	Optional<Term> findByAcademicSessionIdAndName(Long academicSessionId, TermName name);
	Optional<Term> findFirstByCurrentTrue();
	List<Term> findByAcademicSessionIdOrderByStartDateAsc(Long academicSessionId);
	List<Term> findAllByOrderByStartDateAsc();
}
