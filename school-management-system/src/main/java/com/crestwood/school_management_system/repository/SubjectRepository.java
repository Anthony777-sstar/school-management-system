package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
	Optional<Subject> findByName(String name);
	List<Subject> findAllByOrderByNameAsc();
}
