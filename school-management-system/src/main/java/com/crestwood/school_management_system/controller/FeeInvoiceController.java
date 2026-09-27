package com.crestwood.school_management_system.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.FeeInvoiceRequest;
import com.crestwood.school_management_system.dto.ApiDtos.FeeInvoiceResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentFeeSummaryResponse;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.AccountantService;
import com.crestwood.school_management_system.service.FeeInvoiceService;
import com.crestwood.school_management_system.service.StudentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fee-invoices")
public class FeeInvoiceController {
	private final FeeInvoiceService invoiceService;
	private final AccountantService accountantService;
	private final StudentService studentService;

	public FeeInvoiceController(FeeInvoiceService invoiceService, AccountantService accountantService, StudentService studentService) {
		this.invoiceService = invoiceService;
		this.accountantService = accountantService;
		this.studentService = studentService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','ACCOUNTANT','STUDENT','PARENT')")
	public List<?> list(@RequestParam(required = false) Long studentId, @RequestParam(required = false) Long termId) {
		return switch (SecurityUtils.user().getRole()) {
			case SUPER_ADMIN, ACCOUNTANT -> invoiceService.listAll(termId);
			case STUDENT -> {
				var student = studentService.requireStudentForUser(SecurityUtils.user());
				if (studentId != null && !studentId.equals(student.getId())) {
					throw new ForbiddenException("You cannot access another student's invoices");
				}
				yield invoiceService.listForStudent(student, termId);
			}
			case PARENT -> invoiceService.listForParent(studentService.requireParentForUser(SecurityUtils.user()).getId(), studentId, termId);
			case TEACHER -> throw new ForbiddenException("Teachers cannot access fee invoices");
		};
	}

	@PostMapping
	@PreAuthorize("hasRole('ACCOUNTANT')")
	public FeeInvoiceResponse create(@Valid @RequestBody FeeInvoiceRequest request) {
		return invoiceService.create(accountantService.requireAccountantForUser(SecurityUtils.user()), request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ACCOUNTANT')")
	public FeeInvoiceResponse update(@PathVariable Long id, @Valid @RequestBody FeeInvoiceRequest request) {
		return invoiceService.update(accountantService.requireAccountantForUser(SecurityUtils.user()), id, request);
	}
}
