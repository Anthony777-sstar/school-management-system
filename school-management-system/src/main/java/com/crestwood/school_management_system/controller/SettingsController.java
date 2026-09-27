package com.crestwood.school_management_system.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.crestwood.school_management_system.domain.entity.SchoolSettings;
import com.crestwood.school_management_system.dto.ApiDtos.SchoolSettingsRequest;
import com.crestwood.school_management_system.dto.ApiDtos.SchoolSettingsResponse;
import com.crestwood.school_management_system.service.CatalogService;
import com.crestwood.school_management_system.service.LogoStorageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {
	private final CatalogService catalogService;
	private final LogoStorageService logoStorageService;

	public SettingsController(CatalogService catalogService, LogoStorageService logoStorageService) {
		this.catalogService = catalogService;
		this.logoStorageService = logoStorageService;
	}

	@GetMapping("/school-profile")
	public SchoolSettingsResponse schoolProfile() {
		return catalogService.settingsResponse();
	}

	@PutMapping("/school-profile")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public SchoolSettingsResponse updateSchoolProfile(@Valid @RequestBody SchoolSettingsRequest request) {
		return catalogService.updateSettings(request);
	}

	@PostMapping(path = "/school-profile/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public SchoolSettingsResponse uploadLogo(@RequestPart("logo") MultipartFile logo) {
		SchoolSettings current = catalogService.settings();
		String oldFilename = logoStorageService.storedFilename(current.getLogoUrl());
		String logoUrl = logoStorageService.store(logo);
		SchoolSettingsResponse response = catalogService.updateLogo(logoUrl);
		logoStorageService.delete(oldFilename);
		return response;
	}

	@GetMapping("/school-profile/logo/{filename}")
	public ResponseEntity<org.springframework.core.io.Resource> logo(@PathVariable String filename) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(logoStorageService.resource(filename));
	}
}
