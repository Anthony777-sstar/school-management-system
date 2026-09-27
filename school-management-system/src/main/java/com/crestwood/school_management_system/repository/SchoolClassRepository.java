package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.SchoolClass;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
	Optional<SchoolClass> findByName(String name);
	List<SchoolClass> findAllByOrderByNameAsc();
	List<SchoolClass> findByClassTeacherId(Long classTeacherId);
}
