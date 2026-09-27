package com.crestwood.school_management_system;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.dto.ApiDtos.StudyMaterialResponse;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.SubjectRepository;
import com.crestwood.school_management_system.repository.TeacherRepository;
import com.crestwood.school_management_system.repository.UserRepository;
import com.crestwood.school_management_system.service.StudyMaterialService;

@SpringBootTest
@Transactional
class StudyMaterialServiceTests {
	@Autowired
	private StudyMaterialService materialService;
	@Autowired
	private TeacherRepository teacherRepository;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private SubjectRepository subjectRepository;
	@Autowired
	private SchoolClassRepository classRepository;
	@Autowired
	private UserRepository userRepository;

	@Test
	void storesFilesAndEnforcesTeacherAndStudentClassScope() throws Exception {
		Teacher owner = teacherRepository.findByUserEmailIgnoreCase("funmi.adebayo@crestwoodacademy.ng").orElseThrow();
		Teacher otherTeacher = teacherRepository.findByUserEmailIgnoreCase("emeka.chukwu@crestwoodacademy.ng").orElseThrow();
		Student student = studentRepository.findByUserEmailIgnoreCase("tobi.adeyemi@student.crestwoodacademy.ng").orElseThrow();
		User studentUser = userRepository.findByEmailIgnoreCase("tobi.adeyemi@student.crestwoodacademy.ng").orElseThrow();
		User otherTeacherUser = userRepository.findByEmailIgnoreCase("emeka.chukwu@crestwoodacademy.ng").orElseThrow();
		Long subjectId = subjectRepository.findByName("Mathematics").orElseThrow().getId();
		Long classId = classRepository.findByName("JSS 2").orElseThrow().getId();
		byte[] content = "real uploaded material".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile file = new MockMultipartFile("file", "revision.pdf", "application/pdf", content);

		StudyMaterialResponse uploaded = materialService.upload(owner, subjectId, classId, "Revision notes", file);
		Resource resource = materialService.resource(materialService.require(uploaded.id()));
		assertArrayEquals(content, resource.getContentAsByteArray());
		materialService.requireDownloadAccess(materialService.require(uploaded.id()), studentUser);
		assertThrows(ForbiddenException.class, () -> materialService.requireDownloadAccess(materialService.require(uploaded.id()), otherTeacherUser));
		materialService.delete(owner.getUser(), uploaded.id());
		assertThrows(RuntimeException.class, () -> materialService.resource(materialService.require(uploaded.id())));
	}
}
