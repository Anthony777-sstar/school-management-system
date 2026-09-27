package com.crestwood.school_management_system.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.DashboardStatsResponse;
import com.crestwood.school_management_system.dto.ApiDtos.ParentDashboardResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentDashboardResponse;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.DashboardService;
import com.crestwood.school_management_system.service.TeacherService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
	private final DashboardService dashboardService;
	private final TeacherService teacherService;

	public DashboardController(DashboardService dashboardService, TeacherService teacherService) {
		this.dashboardService = dashboardService;
		this.teacherService = teacherService;
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public DashboardStatsResponse admin() {
		return dashboardService.admin();
	}

	@GetMapping("/teacher")
	@PreAuthorize("hasRole('TEACHER')")
	public DashboardStatsResponse teacher() {
		return dashboardService.teacher(teacherService.requireTeacherForUser(SecurityUtils.user()));
	}

	@GetMapping("/student")
	@PreAuthorize("hasRole('STUDENT')")
	public StudentDashboardResponse student() {
		return dashboardService.student(SecurityUtils.user());
	}

	@GetMapping("/parent")
	@PreAuthorize("hasRole('PARENT')")
	public ParentDashboardResponse parent() {
		return dashboardService.parent(SecurityUtils.user());
	}

	@GetMapping("/accountant")
	@PreAuthorize("hasRole('ACCOUNTANT')")
	public DashboardStatsResponse accountant() {
		return dashboardService.accountant();
	}
}
