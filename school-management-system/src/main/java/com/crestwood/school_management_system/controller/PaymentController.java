package com.crestwood.school_management_system.controller;

import java.math.BigDecimal;
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

import com.crestwood.school_management_system.domain.enums.PaymentStatus;
import com.crestwood.school_management_system.dto.ApiDtos.ManualPaymentRequest;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentInitializationRequest;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentInitializationResponse;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentLedgerResponse;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentResponse;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentStatusRequest;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentVerifyRequest;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.security.SecurityUtils;
import com.crestwood.school_management_system.service.AccountantService;
import com.crestwood.school_management_system.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fee-payments")
public class PaymentController {
	private final PaymentService paymentService;
	private final AccountantService accountantService;

	public PaymentController(PaymentService paymentService, AccountantService accountantService) {
		this.paymentService = paymentService;
		this.accountantService = accountantService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','ACCOUNTANT','STUDENT','PARENT')")
	public List<PaymentResponse> list(@RequestParam(required = false) Long studentId, @RequestParam(required = false) Long feeInvoiceId, @RequestParam(required = false) PaymentStatus status) {
		return paymentService.listForUser(SecurityUtils.user(), studentId, feeInvoiceId, status);
	}

	@PostMapping("/initialize")
	@PreAuthorize("hasRole('PARENT')")
	public PaymentInitializationResponse initialize(@Valid @RequestBody PaymentInitializationRequest request) {
		return paymentService.initialize(SecurityUtils.user(), request);
	}

	@PostMapping("/verify")
	@PreAuthorize("hasRole('PARENT')")
	public PaymentResponse verify(@Valid @RequestBody PaymentVerifyRequest request) {
		return paymentService.verify(SecurityUtils.user(), request);
	}

	@PostMapping("/manual")
	@PreAuthorize("hasRole('ACCOUNTANT')")
	public PaymentResponse createManual(@Valid @RequestBody ManualPaymentRequest request) {
		return paymentService.createManual(accountantService.requireAccountantForUser(SecurityUtils.user()), request);
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasRole('ACCOUNTANT')")
	public PaymentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody PaymentStatusRequest request) {
		if (request.status() != PaymentStatus.PAID) {
			throw new BadRequestException("Manual payment status can only be changed to PAID");
		}
		return paymentService.markManualPaid(accountantService.requireAccountantForUser(SecurityUtils.user()), id);
	}

	@GetMapping("/ledger")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','ACCOUNTANT')")
	public PaymentLedgerResponse ledger() {
		List<PaymentResponse> payments = paymentService.listForUser(SecurityUtils.user(), null, null, null);
		BigDecimal total = payments.stream().filter(payment -> payment.status() == PaymentStatus.PAID).map(PaymentResponse::amountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new PaymentLedgerResponse(payments, total);
	}
}
