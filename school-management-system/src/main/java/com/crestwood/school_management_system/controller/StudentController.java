package com.crestwood.school_management_system.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.dto.ApiDtos.RosterUpdateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.StudentProfileResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentUpdateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherSummaryResponse;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.StudentService;
import com.crestwood.school_management_system.service.TeacherService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {
	private final StudentService studentService;
	private final TeacherService teacherService;

	public StudentController(StudentService studentService, TeacherService teacherService) {
		this.studentService = studentService;
		this.teacherService = teacherService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','ACCOUNTANT')")
	public List<StudentResponse> list(@RequestParam(required = false) Long classId, @RequestParam(required = false) String className) {
		return studentService.list(classId, className);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','ACCOUNTANT','TEACHER')")
	public StudentResponse get(@PathVariable Long id) {
		Student student = studentService.requireStudent(id);
		requireTeacherClassAccessIfTeacher(student);
		return studentService.response(student);
	}

	@GetMapping("/me/profile")
	@PreAuthorize("hasRole('STUDENT')")
	public StudentProfileResponse ownProfile() {
		return studentService.profile(studentService.requireStudentForUser(SecurityUtils.user()));
	}

	@GetMapping("/{id}/profile")
	public StudentProfileResponse profile(@PathVariable Long id) {
		Student student = resolveAccessibleStudent(id);
		return studentService.profile(student);
	}

	@GetMapping("/{id}/teachers")
	public List<TeacherSummaryResponse> teachers(@PathVariable Long id) {
		return studentService.teachersForStudent(resolveAccessibleStudent(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
		return studentService.update(id, request);
	}

	@PatchMapping("/{id}/roster")
	@PreAuthorize("hasRole('TEACHER')")
	public StudentResponse updateRoster(@PathVariable Long id, @Valid @RequestBody RosterUpdateRequest request) {
		Teacher teacher = teacherService.requireTeacherForUser(SecurityUtils.user());
		return studentService.updateRoster(teacher, id, request);
	}

	private Student resolveAccessibleStudent(Long id) {
		User user = SecurityUtils.user();
		if (user.getRole() == Role.STUDENT) {
			Student own = studentService.requireStudentForUser(user);
			if (!own.getId().equals(id)) {
				throw new ForbiddenException("You cannot access another student's profile");
			}
			return own;
		}
		if (user.getRole() == Role.PARENT) {
			ParentGuardian parent = studentService.requireParentForUser(user);
			return studentService.requireChild(parent, id);
		}
		Student student = studentService.requireStudent(id);
		if (user.getRole() == Role.TEACHER) {
			requireTeacherClassAccessIfTeacher(student);
		}
		return student;
	}

	private void requireTeacherClassAccessIfTeacher(Student student) {
		User user = SecurityUtils.user();
		if (user.getRole() == Role.TEACHER) {
			Teacher teacher = teacherService.requireTeacherForUser(user);
			if (!teacherService.canAccessClass(teacher, student.getSchoolClass().getId())) {
				throw new ForbiddenException("You are not assigned to this student's class");
			}
		} else if (user.getRole() != Role.SUPER_ADMIN && user.getRole() != Role.ACCOUNTANT && user.getRole() != Role.PARENT && user.getRole() != Role.STUDENT) {
			throw new ForbiddenException("You cannot access student profiles");
		}
	}
}
