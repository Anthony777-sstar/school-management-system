package com.crestwood.school_management_system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.crestwood.school_management_system.domain.entity.FeeInvoice;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentInitializationRequest;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentInitializationResponse;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentResponse;
import com.crestwood.school_management_system.dto.ApiDtos.PaymentVerifyRequest;
import com.crestwood.school_management_system.repository.AccountantRepository;
import com.crestwood.school_management_system.repository.FeeInvoiceRepository;
import com.crestwood.school_management_system.repository.FeePaymentRepository;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.TermRepository;
import com.crestwood.school_management_system.repository.UserRepository;
import com.crestwood.school_management_system.service.FeeInvoiceService;
import com.crestwood.school_management_system.service.PaymentService;

@SpringBootTest
@Transactional
class PaymentServiceTests {
	private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\\"reference\\\":\\\"(crestwood-[^\\\"]+)\\\"");
	@Autowired
	private FeePaymentRepository paymentRepository;
	@Autowired
	private FeeInvoiceRepository invoiceRepository;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private ParentGuardianRepository parentRepository;
	@Autowired
	private StudentParentLinkRepository linkRepository;
	@Autowired
	private AccountantRepository accountantRepository;
	@Autowired
	private FeeInvoiceService invoiceService;
	@Autowired
	private TermRepository termRepository;
	@Autowired
	private UserRepository userRepository;

	@Test
	void initializesAndMarksPaidOnlyAfterServerSidePaystackVerification() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://paystack.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AtomicReference<String> reference = new AtomicReference<>();
		server.expect(once(), requestTo("https://paystack.test/transaction/initialize"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer sk_test_local"))
				.andRespond(request -> {
					Matcher matcher = REFERENCE_PATTERN.matcher(((MockClientHttpRequest) request).getBodyAsString());
					assertNotNull(matcher.find() ? matcher.group(1) : null);
					reference.set(matcher.group(1));
					return jsonResponse("{\"status\":true,\"data\":{\"authorization_url\":\"https://checkout.paystack.test/abc\",\"reference\":\"" + reference.get() + "\",\"email\":\"folake.adeyemi@crestwoodacademy.ng\",\"currency\":\"NGN\",\"amount\":18500000}}");
				});
		server.expect(once(), request -> assertEquals("/transaction/verify/" + reference.get(), request.getURI().getPath()))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer sk_test_local"))
				.andRespond(request -> jsonResponse("{\"status\":true,\"data\":{\"status\":\"success\",\"reference\":\"" + reference.get() + "\",\"email\":\"folake.adeyemi@crestwoodacademy.ng\",\"currency\":\"NGN\",\"amount\":18500000}}"));

		PaymentService service = new PaymentService(builder.build(), paymentRepository, invoiceRepository, studentRepository, parentRepository, linkRepository, accountantRepository, invoiceService, "sk_test_local", "pk_test_local", "NGN");
		User parent = userRepository.findByEmailIgnoreCase("folake.adeyemi@crestwoodacademy.ng").orElseThrow();
		Student student = studentRepository.findByUserEmailIgnoreCase("tobi.adeyemi@student.crestwoodacademy.ng").orElseThrow();
		var currentTerm = termRepository.findFirstByCurrentTrue().orElseThrow();
		FeeInvoice invoice = invoiceRepository.findBySchoolClassIdAndTermId(student.getSchoolClass().getId(), currentTerm.getId()).orElseThrow();

		PaymentInitializationResponse initialization = service.initialize(parent, new PaymentInitializationRequest(invoice.getId(), student.getId()));
		assertNotNull(initialization.paymentId());
		assertEquals(18500000, initialization.amount().movePointRight(2).longValueExact());
		assertEquals(PaymentStatus.PENDING, paymentRepository.findById(initialization.paymentId()).orElseThrow().getStatus());

		PaymentResponse verified = service.verify(parent, new PaymentVerifyRequest(reference.get(), initialization.paymentId(), invoice.getId(), student.getId()));
		assertEquals(PaymentStatus.PAID, verified.status());
		assertEquals(PaymentStatus.PAID, paymentRepository.findById(initialization.paymentId()).orElseThrow().getStatus());
		server.verify();
	}

	private MockClientHttpResponse jsonResponse(String body) {
		MockClientHttpResponse response = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), org.springframework.http.HttpStatus.OK);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		return response;
	}
}
