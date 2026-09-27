package com.crestwood.school_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.StudentParentLink;

public interface StudentParentLinkRepository extends JpaRepository<StudentParentLink, Long> {
	List<StudentParentLink> findByParentIdOrderByIdAsc(Long parentId);
	boolean existsByParentIdAndStudentId(Long parentId, Long studentId);

	@Query("select l.parent from StudentParentLink l where l.student.id = :studentId order by l.parent.fullName")
	List<ParentGuardian> findParentsByStudentId(@Param("studentId") Long studentId);
}
