package com.crestwood.school_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.TeacherSubjectClass;

public interface TeacherSubjectClassRepository extends JpaRepository<TeacherSubjectClass, Long> {
	List<TeacherSubjectClass> findByTeacherId(Long teacherId);
	List<TeacherSubjectClass> findByTeacherIdAndSchoolClassId(Long teacherId, Long schoolClassId);
	List<TeacherSubjectClass> findBySchoolClassId(Long schoolClassId);
	boolean existsByTeacherIdAndSchoolClassId(Long teacherId, Long schoolClassId);
	boolean existsByTeacherIdAndSubjectIdAndSchoolClassId(Long teacherId, Long subjectId, Long schoolClassId);
}
