package com.crestwood.school_management_system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.crestwood.school_management_system.domain.entity.Accountant;
import com.crestwood.school_management_system.domain.entity.FeeInvoice;
import com.crestwood.school_management_system.domain.entity.FeePayment;
import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.PaymentMethod;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.dto.ApiDtos.ManualPaymentRequest;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentInitializationRequest;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentInitializationResponse;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentResponse;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentVerifyRequest;
import com.crestwood.school_management_system.dto.ApiDtos.PaystackConfigResponse;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ConflictException;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.exception.ServiceUnavailableException;
import com.crestwood.school_management_system.repository.AccountantRepository;
import com.crestwood.school_management_system.repository.FeeInvoiceRepository;
import com.crestwood.school_management_system.repository.FeePaymentRepository;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;

@Service
public class PaymentService {
	private final RestClient paystackRestClient;
	private final FeePaymentRepository paymentRepository;
	private final FeeInvoiceRepository invoiceRepository;
	private final StudentRepository studentRepository;
	private final ParentGuardianRepository parentRepository;
	private final StudentParentLinkRepository linkRepository;
	private final AccountantRepository accountantRepository;
	private final FeeInvoiceService invoiceService;
	private final String secretKey;
	private final String publicKey;
	private final String currency;
	private final boolean paystackEnabled;

	public PaymentService(
			RestClient paystackRestClient,
			FeePaymentRepository paymentRepository,
			FeeInvoiceRepository invoiceRepository,
			StudentRepository studentRepository,
			ParentGuardianRepository parentRepository,
			StudentParentLinkRepository linkRepository,
			AccountantRepository accountantRepository,
			FeeInvoiceService invoiceService,
			@Value("${app.paystack.secret-key}") String secretKey,
			@Value("${app.paystack.public-key}") String publicKey,
			@Value("${app.paystack.currency}") String currency,
			@Value("${app.paystack.enabled:false}") boolean paystackEnabled) {
		this.paystackRestClient = paystackRestClient;
		this.paymentRepository = paymentRepository;
		this.invoiceRepository = invoiceRepository;
		this.studentRepository = studentRepository;
		this.parentRepository = parentRepository;
		this.linkRepository = linkRepository;
		this.accountantRepository = accountantRepository;
		this.invoiceService = invoiceService;
		this.secretKey = secretKey;
		this.publicKey = publicKey;
		this.currency = currency;
		this.paystackEnabled = paystackEnabled;
	}

	private void requirePaystackConfigured() {
		if (!paystackEnabled) {
			throw new ServiceUnavailableException("Paystack is not configured. Set PAYSTACK_ENABLED=true with Paystack keys to enable online payment.");
		}
		if (secretKey == null || secretKey.isBlank() || publicKey == null || publicKey.isBlank()) {
			throw new ServiceUnavailableException("Paystack is not configured. Set PAYSTACK_SECRET_KEY and PAYSTACK_PUBLIC_KEY to enable online payment.");
		}
	}

	@Transactional
	public PaymentInitializationResponse initialize(User user, PaymentInitializationRequest request) {
		requireParent(user);
		requirePaystackConfigured();
		Student student = requireAccessibleStudent(user, request.studentId());
		FeeInvoice invoice = invoiceRepository.findByIdForUpdate(request.feeInvoiceId()).orElseThrow(() -> new NotFoundException("Fee invoice not found"));
		requireInvoiceForStudent(invoice, student);
		BigDecimal outstanding = invoiceService.outstandingFor(invoice, student);
		if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BadRequestException("This invoice has already been paid");
		}
		String reference = "crestwood-" + UUID.randomUUID();
		long amountKobo = toKobo(outstanding);
		Map<String, Object> response = callPaystack("/transaction/initialize", Map.of("email", user.getEmail(), "amount", amountKobo, "currency", currency, "reference", reference), false);
		Map<String, Object> data = requiredData(response);
		String authorizationUrl = requiredText(data, "authorization_url");
		String responseReference = requiredText(data, "reference");
		String responseEmail = requiredText(data, "email");
		String responseCurrency = requiredText(data, "currency");
		long responseAmount = requiredLong(data, "amount");
		if (!responseReference.equals(reference) || !responseEmail.equalsIgnoreCase(user.getEmail()) || !responseCurrency.equalsIgnoreCase(currency) || responseAmount != amountKobo) {
			throw new ServiceUnavailableException("Paystack returned inconsistent initialization data");
		}
		FeePayment payment = paymentRepository.save(new FeePayment(student, invoice, outstanding, PaymentMethod.PAYSTACK, reference, PaymentStatus.PENDING, null));
		return new PaymentInitializationResponse(payment.getId(), reference, authorizationUrl, publicKey, outstanding, currency, user.getEmail(), Instant.now().plusSeconds(1800));
	}

	@Transactional
	public PaymentResponse verify(User user, PaymentVerifyRequest request) {
		requireParent(user);
		requirePaystackConfigured();
		FeePayment payment = paymentRepository.findByPaystackReference(request.reference()).orElseThrow(() -> new NotFoundException("Payment reference not found"));
		requirePaymentAccess(user, payment);
		if (request.studentId() != null && !request.studentId().equals(payment.getStudent().getId())) {
			throw new BadRequestException("Payment reference does not belong to the supplied student");
		}
		if (request.feeInvoiceId() != null && !request.feeInvoiceId().equals(payment.getFeeInvoice().getId())) {
			throw new BadRequestException("Payment reference does not belong to the supplied invoice");
		}
		if (request.paymentId() != null && !request.paymentId().equals(payment.getId()) && !request.paymentId().equals(payment.getFeeInvoice().getId())) {
			throw new BadRequestException("Payment reference does not belong to the supplied payment");
		}
		FeeInvoice lockedInvoice = invoiceRepository.findByIdForUpdate(payment.getFeeInvoice().getId()).orElseThrow(() -> new NotFoundException("Fee invoice not found"));
		FeePayment locked = paymentRepository.findByIdForUpdate(payment.getId()).orElseThrow(() -> new NotFoundException("Payment not found"));
		if (locked.getStatus() == PaymentStatus.PAID) {
			return response(locked);
		}
		if (locked.getPaymentMethod() != PaymentMethod.PAYSTACK) {
			throw new BadRequestException("Only Paystack payments can be verified");
		}
		BigDecimal outstanding = invoiceService.outstandingFor(lockedInvoice, locked.getStudent());
		if (locked.getAmountPaid().compareTo(outstanding) > 0) {
			throw new ConflictException("Payment amount exceeds the outstanding invoice amount");
		}
		Map<String, Object> response = callPaystack("/transaction/verify/" + locked.getPaystackReference(), null, true);
		Map<String, Object> data = requiredData(response);
		if (!"success".equalsIgnoreCase(requiredText(data, "status"))) {
			throw new BadRequestException("Paystack has not confirmed this transaction");
		}
		String verifiedReference = requiredText(data, "reference");
		String verifiedEmail = requiredText(data, "email");
		String verifiedCurrency = requiredText(data, "currency");
		long verifiedAmount = requiredLong(data, "amount");
		if (!locked.getPaystackReference().equals(verifiedReference) || !user.getEmail().equalsIgnoreCase(verifiedEmail) || !currency.equalsIgnoreCase(verifiedCurrency) || verifiedAmount != toKobo(locked.getAmountPaid())) {
			throw new BadRequestException("Paystack transaction details do not match the server payment record");
		}
		locked.setStatus(PaymentStatus.PAID);
		locked.setPaidAt(Instant.now());
		return response(paymentRepository.save(locked));
	}

	@Transactional
	public PaymentResponse createManual(Accountant accountant, ManualPaymentRequest request) {
		if (request.paymentMethod() == PaymentMethod.PAYSTACK) {
			throw new BadRequestException("Paystack payments must be initialized and verified by Paystack");
		}
		Student student = studentRepository.findById(request.studentId()).orElseThrow(() -> new NotFoundException("Student not found"));
		FeeInvoice invoice = invoiceRepository.findByIdForUpdate(request.feeInvoiceId()).orElseThrow(() -> new NotFoundException("Fee invoice not found"));
		requireInvoiceForStudent(invoice, student);
		BigDecimal amount = money(request.amountPaid());
		BigDecimal outstanding = invoiceService.outstandingFor(invoice, student);
		if (amount.compareTo(outstanding) > 0) {
			throw new BadRequestException("Manual payment cannot exceed the outstanding invoice amount");
		}
		return response(paymentRepository.save(new FeePayment(student, invoice, amount, request.paymentMethod(), null, PaymentStatus.PENDING, accountant)));
	}

	@Transactional
	public PaymentResponse markManualPaid(Accountant accountant, Long paymentId) {
		FeePayment located = paymentRepository.findById(paymentId).orElseThrow(() -> new NotFoundException("Payment not found"));
		FeeInvoice invoice = invoiceRepository.findByIdForUpdate(located.getFeeInvoice().getId()).orElseThrow(() -> new NotFoundException("Fee invoice not found"));
		FeePayment payment = paymentRepository.findByIdForUpdate(paymentId).orElseThrow(() -> new NotFoundException("Payment not found"));
		if (payment.getPaymentMethod() == PaymentMethod.PAYSTACK) {
			throw new BadRequestException("Paystack payments cannot be marked manually");
		}
		if (payment.getStatus() == PaymentStatus.PAID) {
			return response(payment);
		}
		BigDecimal outstanding = invoiceService.outstandingFor(invoice, payment.getStudent());
		if (payment.getAmountPaid().compareTo(outstanding) > 0) {
			throw new ConflictException("Payment amount exceeds the outstanding invoice amount");
		}
		payment.setStatus(PaymentStatus.PAID);
		payment.setPaidAt(Instant.now());
		payment.setRecordedByAccountant(accountant);
		return response(paymentRepository.save(payment));
	}

	@Transactional(readOnly = true)
	public List<PaymentResponse> listForUser(User user, Long studentId, Long feeInvoiceId, PaymentStatus status) {
		List<FeePayment> payments = switch (user.getRole()) {
			case SUPER_ADMIN, ACCOUNTANT -> paymentRepository.findAllByOrderByIdDesc();
			case PARENT -> linkedPayments(user);
			case STUDENT -> {
				Student ownStudent = requireStudentForUser(user);
				if (studentId != null && !studentId.equals(ownStudent.getId())) {
					throw new ForbiddenException("You cannot access another student's payments");
				}
				yield paymentRepository.findByStudentIdOrderByCreatedAtAscIdAsc(ownStudent.getId());
			}
			case TEACHER -> throw new ForbiddenException("Teachers cannot access fee payments");
		};
		return payments.stream().filter(payment -> studentId == null || payment.getStudent().getId().equals(studentId)).filter(payment -> feeInvoiceId == null || payment.getFeeInvoice().getId().equals(feeInvoiceId)).filter(payment -> status == null || payment.getStatus() == status).map(this::response).toList();
	}

	public PaymentResponse response(FeePayment payment) {
		return new PaymentResponse(payment.getId(), payment.getStudent().getId(), payment.getStudent().getFullName(), payment.getFeeInvoice().getId(), payment.getFeeInvoice().getSchoolClass().getName(), payment.getFeeInvoice().getTerm().getName().name(), payment.getAmountPaid(), payment.getPaymentMethod(), payment.getPaystackReference(), payment.getStatus(), payment.getPaidAt(), payment.getRecordedByAccountant() == null ? null : payment.getRecordedByAccountant().getId(), payment.getCreatedAt());
	}

	public PaystackConfigResponse paystackConfig() {
		requirePaystackConfigured();
		return new PaystackConfigResponse(publicKey, currency);
	}

	private List<FeePayment> linkedPayments(User user) {
		ParentGuardian parent = parentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Parent profile not found"));
		return linkRepository.findByParentIdOrderByIdAsc(parent.getId()).stream().flatMap(link -> paymentRepository.findByStudentIdOrderByCreatedAtAscIdAsc(link.getStudent().getId()).stream()).toList();
	}

	private Student requireAccessibleStudent(User user, Long studentId) {
		Student student = studentRepository.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
		ParentGuardian parent = parentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Parent profile not found"));
		if (!linkRepository.existsByParentIdAndStudentId(parent.getId(), studentId)) {
			throw new ForbiddenException("This student is not linked to your account");
		}
		return student;
	}

	private Student requireStudentForUser(User user) {
		return studentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Student profile not found"));
	}

	private void requirePaymentAccess(User user, FeePayment payment) {
		ParentGuardian parent = parentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Parent profile not found"));
		if (!linkRepository.existsByParentIdAndStudentId(parent.getId(), payment.getStudent().getId())) {
			throw new ForbiddenException("This payment is not linked to your account");
		}
	}

	private void requireParent(User user) {
		if (user.getRole() != Role.PARENT) {
			throw new ForbiddenException("Only parents can initialize or verify Paystack payments");
		}
	}

	private void requireInvoiceForStudent(FeeInvoice invoice, Student student) {
		if (!invoice.getSchoolClass().getId().equals(student.getSchoolClass().getId())) {
			throw new BadRequestException("The invoice is not assigned to this student's class");
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> callPaystack(String path, Map<String, Object> body, boolean verification) {
		try {
			Map<String, Object> response;
			if (body == null) {
				response = paystackRestClient.get().uri(path).header("Authorization", "Bearer " + secretKey).retrieve().body(Map.class);
			} else {
				response = paystackRestClient.post().uri(path).header("Authorization", "Bearer " + secretKey).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
			}
			if (response == null || !Boolean.TRUE.equals(response.get("status"))) {
				if (verification) {
					throw new BadRequestException("Paystack has not confirmed this transaction");
				}
				throw new ServiceUnavailableException("Paystack returned an unsuccessful response");
			}
			return response;
		} catch (BadRequestException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ServiceUnavailableException("Paystack service is unavailable");
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> requiredData(Map<String, Object> response) {
		Object data = response == null ? null : response.get("data");
		if (!(data instanceof Map<?, ?>)) {
			throw new ServiceUnavailableException("Paystack returned no transaction data");
		}
		return (Map<String, Object>) data;
	}

	private String requiredText(Map<String, Object> data, String field) {
		Object value = data.get(field);
		if (value == null || value.toString().isBlank()) {
			throw new ServiceUnavailableException("Paystack response is missing " + field);
		}
		return value.toString();
	}

	private long requiredLong(Map<String, Object> data, String field) {
		Object value = data.get(field);
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(value));
		} catch (RuntimeException exception) {
			throw new ServiceUnavailableException("Paystack response is missing " + field);
		}
	}

	private long toKobo(BigDecimal amount) {
		try {
			return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
		} catch (ArithmeticException exception) {
			throw new BadRequestException("Payment amount has more than two decimal places");
		}
	}

	private BigDecimal money(BigDecimal amount) {
		try {
			return amount.setScale(2, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException exception) {
			throw new BadRequestException("Payment amount has more than two decimal places");
		}
	}
}
