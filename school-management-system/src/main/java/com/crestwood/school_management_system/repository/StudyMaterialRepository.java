package com.crestwood.school_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.StudyMaterial;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
	List<StudyMaterial> findByTeacherIdOrderByUploadedAtDesc(Long teacherId);
	List<StudyMaterial> findBySchoolClassIdOrderByUploadedAtDesc(Long schoolClassId);
	List<StudyMaterial> findBySchoolClassIdAndSubjectIdOrderByUploadedAtDesc(Long schoolClassId, Long subjectId);
	List<StudyMaterial> findAllByOrderByUploadedAtDesc();
}
