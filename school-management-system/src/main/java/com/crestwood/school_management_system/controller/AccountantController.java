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
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.AccountantCreateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.AccountantResponse;
import com.crestwood.school_management_system.dto.ApiDtos.AccountantUpdateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.MessageResponse;
import com.crestwood.school_management_system.service.AccountantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accountants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AccountantController {
	private final AccountantService accountantService;

	public AccountantController(AccountantService accountantService) {
		this.accountantService = accountantService;
	}

	@GetMapping
	public List<AccountantResponse> list() {
		return accountantService.list();
	}

	@PostMapping
	public AccountantResponse create(@Valid @RequestBody AccountantCreateRequest request) {
		return accountantService.create(request);
	}

	@PutMapping("/{id}")
	public AccountantResponse update(@PathVariable Long id, @Valid @RequestBody AccountantUpdateRequest request) {
		return accountantService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public MessageResponse disable(@PathVariable Long id) {
		accountantService.disable(id);
		return new MessageResponse("Accountant account disabled");
	}
}
