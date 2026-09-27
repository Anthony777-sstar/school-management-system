package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.Role;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmailIgnoreCase(String email);
	boolean existsByEmailIgnoreCase(String email);
	List<User> findByRoleOrderByCreatedAtAsc(Role role);
}
