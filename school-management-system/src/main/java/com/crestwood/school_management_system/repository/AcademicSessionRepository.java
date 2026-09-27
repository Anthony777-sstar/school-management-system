package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.AcademicSession;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
	Optional<AcademicSession> findByName(String name);
	Optional<AcademicSession> findFirstByCurrentTrue();
	List<AcademicSession> findAllByOrderByNameDesc();
}
