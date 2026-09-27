package com.crestwood.school_management_system.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.MessageResponse;
import com.crestwood.school_management_system.dto.ApiDtos.NoticeboardCreateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.NoticeboardResponse;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.NoticeboardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/noticeboard-posts")
public class NoticeboardController {
	private final NoticeboardService noticeboardService;

	public NoticeboardController(NoticeboardService noticeboardService) {
		this.noticeboardService = noticeboardService;
	}

	@GetMapping
	public List<NoticeboardResponse> list(@RequestParam(required = false) String audience) {
		return noticeboardService.list(SecurityUtils.user());
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
	public NoticeboardResponse create(@Valid @RequestBody NoticeboardCreateRequest request) {
		return noticeboardService.create(SecurityUtils.user(), request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
	public MessageResponse delete(@PathVariable Long id) {
		noticeboardService.delete(SecurityUtils.user(), id);
		return new MessageResponse("Noticeboard post deleted");
	}
}
