package com.crestwood.school_management_system.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.FeeReminderRequest;
import com.crestwood.school_management_system.dto.ApiDtos.MessageResponse;
import com.crestwood.school_management_system.service.FeeInvoiceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fee-reminders")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ACCOUNTANT')")
public class FeeReminderController {
	private final FeeInvoiceService invoiceService;

	public FeeReminderController(FeeInvoiceService invoiceService) {
		this.invoiceService = invoiceService;
	}

	@PostMapping
	public MessageResponse send(@Valid @RequestBody FeeReminderRequest request) {
		invoiceService.sendReminder(request.studentId(), request.invoiceId());
		return new MessageResponse(invoiceService.isMailEnabled() ? "Fee reminder emailed to the linked parent or guardian" : "Fee reminder recorded in the parent portal. Email delivery is disabled, so no email was sent.");
	}
}
