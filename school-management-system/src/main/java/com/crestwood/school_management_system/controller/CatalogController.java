package com.crestwood.school_management_system.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.AcademicContextResponse;
import com.crestwood.school_management_system.dto.ApiDtos.AcademicSessionCurrentRequest;
import com.crestwood.school_management_system.dto.ApiDtos.AcademicSessionRequest;
import com.crestwood.school_management_system.dto.ApiDtos.AcademicSessionResponse;
import com.crestwood.school_management_system.dto.ApiDtos.SchoolClassResponse;
import com.crestwood.school_management_system.dto.ApiDtos.SubjectResponse;
import com.crestwood.school_management_system.dto.ApiDtos.TermCurrentRequest;
import com.crestwood.school_management_system.dto.ApiDtos.TermRequest;
import com.crestwood.school_management_system.dto.ApiDtos.TermResponse;
import com.crestwood.school_management_system.service.CatalogService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
	private final CatalogService catalogService;

	public CatalogController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping("/school-classes")
	public List<SchoolClassResponse> classes() {
		return catalogService.classes();
	}

	@GetMapping("/subjects")
	public List<SubjectResponse> subjects() {
		return catalogService.subjects();
	}

	@GetMapping("/academic-sessions")
	public List<AcademicSessionResponse> sessions() {
		return catalogService.sessions();
	}

	@PostMapping("/academic-sessions")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public AcademicSessionResponse createSession(@Valid @RequestBody AcademicSessionRequest request) {
		return catalogService.createSession(request);
	}

	@PutMapping("/academic-sessions/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public AcademicSessionResponse updateSession(@PathVariable Long id, @Valid @RequestBody AcademicSessionRequest request) {
		return catalogService.updateSession(id, request);
	}

	@PatchMapping("/academic-sessions/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public AcademicSessionResponse markSessionCurrent(@PathVariable Long id, @Valid @RequestBody AcademicSessionCurrentRequest request) {
		return catalogService.markSessionCurrent(id, request.current());
	}

	@GetMapping("/terms")
	public List<TermResponse> terms() {
		return catalogService.terms();
	}

	@PostMapping("/terms")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public TermResponse createTerm(@Valid @RequestBody TermRequest request) {
		return catalogService.createTerm(request);
	}

	@PutMapping("/terms/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public TermResponse updateTerm(@PathVariable Long id, @Valid @RequestBody TermRequest request) {
		return catalogService.updateTerm(id, request);
	}

	@PatchMapping("/terms/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public TermResponse markTermCurrent(@PathVariable Long id, @Valid @RequestBody TermCurrentRequest request) {
		return catalogService.markTermCurrent(id, request.current());
	}

	@GetMapping("/academic-context")
	public AcademicContextResponse academicContext() {
		return catalogService.academicContext();
	}
}
