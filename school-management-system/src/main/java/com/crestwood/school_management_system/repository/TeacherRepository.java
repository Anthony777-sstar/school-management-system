package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.Teacher;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
	Optional<Teacher> findByUserId(Long userId);
	Optional<Teacher> findByUserEmailIgnoreCase(String email);
	List<Teacher> findAllByOrderByFullNameAsc();
}
