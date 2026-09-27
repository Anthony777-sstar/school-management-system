package com.crestwood.school_management_system.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.OutstandingFeeResponse;
import com.crestwood.school_management_system.service.FeeInvoiceService;

@RestController
@RequestMapping("/api/v1/outstanding-fees")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ACCOUNTANT')")
public class OutstandingFeeController {
	private final FeeInvoiceService invoiceService;

	public OutstandingFeeController(FeeInvoiceService invoiceService) {
		this.invoiceService = invoiceService;
	}

	@GetMapping
	public List<OutstandingFeeResponse> list(@RequestParam(required = false) Long termId, @RequestParam(required = false) Long classId) {
		return invoiceService.outstanding(termId, classId);
	}
}
