package com.crestwood.school_management_system.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.crestwood.school_management_system.domain.entity.StudyMaterial;
import com.crestwood.school_management_system.dto.ApiDtos.MessageResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudyMaterialResponse;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.StudyMaterialService;
import com.crestwood.school_management_system.service.TeacherService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/study-materials")
@Validated
public class StudyMaterialController {
	private final StudyMaterialService studyMaterialService;
	private final TeacherService teacherService;

	public StudyMaterialController(StudyMaterialService studyMaterialService, TeacherService teacherService) {
		this.studyMaterialService = studyMaterialService;
		this.teacherService = teacherService;
	}

	@GetMapping
	public java.util.List<StudyMaterialResponse> list(@RequestParam(required = false) Long classId, @RequestParam(required = false) Long subjectId) {
		return studyMaterialService.listForUser(SecurityUtils.user(), classId, subjectId);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('TEACHER')")
	public StudyMaterialResponse upload(
			@RequestParam @NotNull Long classId,
			@RequestParam @NotNull Long subjectId,
			@RequestParam @NotBlank @Size(max = 200) String title,
			@RequestPart("file") MultipartFile file) {
		return studyMaterialService.upload(teacherService.requireTeacherForUser(SecurityUtils.user()), subjectId, classId, title, file);
	}

	@GetMapping("/{id}/download")
	public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
		StudyMaterial material = studyMaterialService.require(id);
		studyMaterialService.requireDownloadAccess(material, SecurityUtils.user());
		org.springframework.core.io.Resource resource = studyMaterialService.resource(material);
		ContentDisposition disposition = ContentDisposition.attachment().filename(material.getOriginalFilename(), StandardCharsets.UTF_8).build();
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(resource);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('TEACHER','SUPER_ADMIN')")
	public MessageResponse delete(@PathVariable Long id) {
		studyMaterialService.delete(SecurityUtils.user(), id);
		return new MessageResponse("Study material deleted");
	}
}
