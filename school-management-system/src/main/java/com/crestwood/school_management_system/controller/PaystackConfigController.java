package com.crestwood.school_management_system.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crestwood.school_management_system.dto.ApiDtos.PaystackConfigResponse;
import com.crestwood.school_management_system.service.PaymentService;

@RestController
@RequestMapping("/api/v1")
public class PaystackConfigController {
	private final PaymentService paymentService;

	public PaystackConfigController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@GetMapping("/config/paystack")
	@PreAuthorize("hasRole('PARENT')")
	public PaystackConfigResponse config() {
		return paymentService.paystackConfig();
	}

	@GetMapping("/payments/config")
	@PreAuthorize("hasRole('PARENT')")
	public PaystackConfigResponse alternateConfig() {
		return paymentService.paystackConfig();
	}
}
