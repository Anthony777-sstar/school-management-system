package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.ParentGuardian;

public interface ParentGuardianRepository extends JpaRepository<ParentGuardian, Long> {
	Optional<ParentGuardian> findByUserId(Long userId);
	Optional<ParentGuardian> findByUserEmailIgnoreCase(String email);
	List<ParentGuardian> findAllByOrderByFullNameAsc();
}
