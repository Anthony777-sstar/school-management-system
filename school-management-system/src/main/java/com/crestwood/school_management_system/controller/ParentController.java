package com.crestwood.school_management_system.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.ParentChildResponse;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.StudentService;

@RestController
@RequestMapping("/api/v1/parents")
public class ParentController {
	private final StudentService studentService;

	public ParentController(StudentService studentService) {
		this.studentService = studentService;
	}

	@GetMapping("/me/children")
	@PreAuthorize("hasRole('PARENT')")
	public List<ParentChildResponse> children() {
		return studentService.children(studentService.requireParentForUser(SecurityUtils.user()));
	}
}
