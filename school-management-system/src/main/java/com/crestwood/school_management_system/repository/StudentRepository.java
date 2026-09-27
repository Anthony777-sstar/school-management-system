package com.crestwood.school_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.enums.RosterFlag;

public interface StudentRepository extends JpaRepository<Student, Long> {
	Optional<Student> findByUserId(Long userId);
	Optional<Student> findByUserEmailIgnoreCase(String email);
	List<Student> findAllByOrderByFullNameAsc();
	List<Student> findBySchoolClassIdOrderByFullNameAsc(Long schoolClassId);
	List<Student> findBySchoolClassIdAndRosterConfirmedOrderByFullNameAsc(Long schoolClassId, boolean rosterConfirmed);
	List<Student> findBySchoolClassIdAndRosterConfirmedAndRosterFlagOrderByFullNameAsc(Long schoolClassId, boolean rosterConfirmed, RosterFlag rosterFlag);
	long countBySchoolClassIdAndRosterConfirmed(Long schoolClassId, boolean rosterConfirmed);
}
