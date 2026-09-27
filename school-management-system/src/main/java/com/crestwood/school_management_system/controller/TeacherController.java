package com.crestwood.school_management_system.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.MessageResponse;
import com.crestwood.school_management_system.dto.ApiDtos.SchoolClassResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentResponse;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherAssignmentRequest;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherClassSubjectResponse;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherCreateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherResponse;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherUpdateRequest;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.StudentService;
import com.crestwood.school_management_system.service.TeacherService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class TeacherController {
	private final TeacherService teacherService;
	private final StudentService studentService;

	public TeacherController(TeacherService teacherService, StudentService studentService) {
		this.teacherService = teacherService;
		this.studentService = studentService;
	}

	@GetMapping("/teachers")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public List<TeacherResponse> list() {
		return teacherService.list();
	}

	@PostMapping("/teachers")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public TeacherResponse create(@Valid @RequestBody TeacherCreateRequest request) {
		return teacherService.create(request);
	}

	@PutMapping("/teachers/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherUpdateRequest request) {
		return teacherService.update(id, request);
	}

	@DeleteMapping("/teachers/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public MessageResponse disable(@PathVariable Long id) {
		teacherService.disable(id);
		return new MessageResponse("Teacher account disabled");
	}

	@PostMapping("/teachers/{teacherId}/assignments")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public TeacherResponse addAssignment(@PathVariable Long teacherId, @Valid @RequestBody TeacherAssignmentRequest request) {
		return teacherService.addAssignment(teacherId, request.subjectId(), request.schoolClassId());
	}

	@DeleteMapping("/teachers/{teacherId}/assignments/{assignmentId}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public MessageResponse removeAssignment(@PathVariable Long teacherId, @PathVariable Long assignmentId) {
		teacherService.removeAssignment(teacherId, assignmentId);
		return new MessageResponse("Teacher assignment removed");
	}

	@GetMapping("/teachers/me/classes")
	@PreAuthorize("hasRole('TEACHER')")
	public List<TeacherClassSubjectResponse> myClasses() {
		return teacherService.classesForTeacher(teacherService.requireTeacherForUser(SecurityUtils.user()));
	}

	@GetMapping("/teachers/me/roster")
	@PreAuthorize("hasRole('TEACHER')")
	public List<StudentResponse> myRoster(@RequestParam(required = false) Long classId, @RequestParam(defaultValue = "PENDING") String rosterStatus) {
		return studentService.awaitingRoster(teacherService.requireTeacherForUser(SecurityUtils.user()), classId, rosterStatus);
	}
}
