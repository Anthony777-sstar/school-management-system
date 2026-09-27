package com.crestwood.school_management_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.SchoolSettings;

public interface SchoolSettingsRepository extends JpaRepository<SchoolSettings, Long> {
	Optional<SchoolSettings> findFirstByOrderByIdAsc();
}
