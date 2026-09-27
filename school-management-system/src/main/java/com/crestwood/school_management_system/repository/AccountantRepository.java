package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.Accountant;

public interface AccountantRepository extends JpaRepository<Accountant, Long> {
	Optional<Accountant> findByUserId(Long userId);
	Optional<Accountant> findByUserEmailIgnoreCase(String email);
	List<Accountant> findAllByOrderByFullNameAsc();
}
