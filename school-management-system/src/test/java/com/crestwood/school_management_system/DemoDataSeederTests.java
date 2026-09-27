package com.crestwood.school_management_system.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.FeePayment;
import com.crestwood.school_management_system.domain.entity.Result;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.enums.EnrollmentStatus;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;
import com.crestwood.school_management_system.domain.enums.TermName;
import com.crestwood.school_management_system.repository.AcademicSessionRepository;
import com.crestwood.school_management_system.repository.AccountantRepository;
import com.crestwood.school_management_system.repository.EnrollmentApplicationRepository;
import com.crestwood.school_management_system.repository.FeeInvoiceRepository;
import com.crestwood.school_management_system.repository.FeePaymentRepository;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.ResultRepository;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.SubjectRepository;
import com.crestwood.school_management_system.repository.TeacherRepository;
import com.crestwood.school_management_system.repository.TermRepository;
import com.crestwood.school_management_system.repository.UserRepository;

@SpringBootTest
@Transactional
class DemoDataSeederTests {
	@Autowired
	private DemoDataSeeder seeder;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private TeacherRepository teacherRepository;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private ParentGuardianRepository parentRepository;
	@Autowired
	private AccountantRepository accountantRepository;
	@Autowired
	private SchoolClassRepository classRepository;
	@Autowired
	private SubjectRepository subjectRepository;
	@Autowired
	private AcademicSessionRepository sessionRepository;
	@Autowired
	private TermRepository termRepository;
	@Autowired
	private EnrollmentApplicationRepository enrollmentRepository;
	@Autowired
	private ResultRepository resultRepository;
	@Autowired
	private FeeInvoiceRepository invoiceRepository;
	@Autowired
	private FeePaymentRepository paymentRepository;
	@Autowired
	private StudentParentLinkRepository linkRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void seedsExactDatasetIdempotently() {
		seeder.run(new DefaultApplicationArguments(new String[0]));
		seeder.run(new DefaultApplicationArguments(new String[0]));

		assertEquals(9, userRepository.count());
		assertEquals(3, teacherRepository.count());
		assertEquals(2, studentRepository.count());
		assertEquals(2, parentRepository.count());
		assertEquals(1, accountantRepository.count());
		assertEquals(6, classRepository.count());
		assertEquals(10, subjectRepository.count());
		assertEquals(1, sessionRepository.count());
		assertEquals(3, termRepository.count());
		assertEquals(1, enrollmentRepository.count());
		assertEquals(2, resultRepository.count());
		assertEquals(2, invoiceRepository.count());
		assertEquals(2, paymentRepository.count());
		assertEquals(2, linkRepository.count());
		assertEquals(List.of("JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3"), classRepository.findAllByOrderByNameAsc().stream().map(item -> item.getName()).toList());
		assertEquals("2025/2026", sessionRepository.findFirstByCurrentTrue().orElseThrow().getName());
		assertEquals(TermName.SECOND_TERM, termRepository.findFirstByCurrentTrue().orElseThrow().getName());
		assertTrue(studentRepository.findByUserEmailIgnoreCase("tobi.adeyemi@student.crestwoodacademy.ng").orElseThrow().isRosterConfirmed());
		assertTrue(studentRepository.findByUserEmailIgnoreCase("halima.suleiman@student.crestwoodacademy.ng").orElseThrow().isRosterConfirmed());
		assertTrue(passwordEncoder.matches("test-only-seed-password", userRepository.findByEmailIgnoreCase("admin@crestwoodacademy.ng").orElseThrow().getPassword()));
		assertTrue(enrollmentRepository.findByStatusOrderBySubmittedAtDesc(EnrollmentStatus.PENDING).stream().anyMatch(item -> item.getStudentFullName().equals("Emeka Okoro") && item.getApplyingForClass().getName().equals("JSS 1")));

		Student tobi = studentRepository.findByUserEmailIgnoreCase("tobi.adeyemi@student.crestwoodacademy.ng").orElseThrow();
		Student halima = studentRepository.findByUserEmailIgnoreCase("halima.suleiman@student.crestwoodacademy.ng").orElseThrow();
		Result tobiResult = resultRepository.findByStudentIdOrderByIdAsc(tobi.getId()).get(0);
		Result halimaResult = resultRepository.findByStudentIdOrderByIdAsc(halima.getId()).get(0);
		assertResult(tobiResult, 16, 17, 48, 81, "A");
		assertResult(halimaResult, 14, 15, 40, 69, "B");
		assertEquals(new BigDecimal("185000.00"), invoiceRepository.findBySchoolClassIdAndTermId(tobi.getSchoolClass().getId(), tobiResult.getTerm().getId()).orElseThrow().getAmount());
		List<FeePayment> payments = paymentRepository.findByStudentIdOrderByCreatedAtAscIdAsc(tobi.getId());
		assertEquals(2, payments.size());
		assertTrue(payments.stream().anyMatch(payment -> payment.getStatus() == PaymentStatus.PAID));
		assertTrue(payments.stream().anyMatch(payment -> payment.getStatus() == PaymentStatus.PENDING));
		assertFalse(payments.stream().allMatch(payment -> payment.getStatus() == PaymentStatus.PAID));
	}

	private void assertResult(Result result, int ca1, int ca2, int exam, int total, String grade) {
		assertEquals(ca1, result.getCa1());
		assertEquals(ca2, result.getCa2());
		assertEquals(exam, result.getExam());
		assertEquals(total, result.getTotal());
		assertEquals(grade, result.getGrade());
	}
}
