package com.crestwood.school_management_system.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.domain.enums.EnrollmentStatus;
import com.crestwood.school_management_system.dto.ApiDtos.ApprovalResponse;
import com.crestwood.school_management_system.dto.ApiDtos.EnrollmentApplicationRequest;
import com.crestwood.school_management_system.dto.ApiDtos.EnrollmentApplicationResponse;
import com.crestwood.school_management_system.dto.ApiDtos.EnrollmentReviewRequest;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.EnrollmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/enrollment-applications")
public class EnrollmentApplicationController {
	private final EnrollmentService enrollmentService;

	public EnrollmentApplicationController(EnrollmentService enrollmentService) {
		this.enrollmentService = enrollmentService;
	}

	@PostMapping
	public EnrollmentApplicationResponse create(@Valid @RequestBody EnrollmentApplicationRequest request) {
		return enrollmentService.create(request);
	}

	@GetMapping
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public List<EnrollmentApplicationResponse> list(@RequestParam(required = false) EnrollmentStatus status) {
		return enrollmentService.list(status);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public EnrollmentApplicationResponse get(@PathVariable Long id) {
		return enrollmentService.get(id);
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public ApprovalResponse review(@PathVariable Long id, @Valid @RequestBody EnrollmentReviewRequest request) {
		return enrollmentService.review(id, request, SecurityUtils.user());
	}
}
