package com.crestwood.school_management_system.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.ResultBatchRequest;
import com.crestwood.school_management_system.dto.ApiDtos.ResultResponse;
import com.crestwood.school_management_system.dto.ApiDtos.ResultScoreRequest;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.ResultService;
import com.crestwood.school_management_system.service.TeacherService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/results")
public class ResultController {
	private final ResultService resultService;
	private final TeacherService teacherService;

	public ResultController(ResultService resultService, TeacherService teacherService) {
		this.resultService = resultService;
		this.teacherService = teacherService;
	}

	@GetMapping
	public List<ResultResponse> list(
			@RequestParam(required = false) Long studentId,
			@RequestParam(required = false) Long classId,
			@RequestParam(required = false) Long subjectId,
			@RequestParam(required = false) Long termId,
			@RequestParam(required = false) String status) {
		return resultService.listForUser(SecurityUtils.user(), studentId, classId, subjectId, termId, status);
	}

	@PostMapping
	@PreAuthorize("hasRole('TEACHER')")
	public ResultResponse create(@Valid @RequestBody ResultScoreRequest request) {
		return resultService.save(teacherService.requireTeacherForUser(SecurityUtils.user()), request);
	}

	@PutMapping
	@PreAuthorize("hasRole('TEACHER')")
	public List<ResultResponse> saveBatch(@Valid @RequestBody ResultBatchRequest request) {
		return resultService.saveBatch(teacherService.requireTeacherForUser(SecurityUtils.user()), request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public void delete(@PathVariable Long id) {
		resultService.delete(id);
	}

}
