package com.crestwood.school_management_system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.SchoolClass;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.Term;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.EnrollmentStatus;
import com.crestwood.school_management_system.domain.enums.NoticeboardAudience;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;
import com.crestwood.school_management_system.domain.enums.TermName;
import com.crestwood.school_management_system.dto.ApiDtos.ApprovalResponse;
import com.crestwood.school_management_system.dto.ApiDtos.EnrollmentApplicationRequest;
import com.crestwood.school_management_system.dto.ApiDtos.NoticeboardCreateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.EnrollmentReviewRequest;
import com.crestwood.school_management_system.dto.ApiDtos.ForgotPasswordRequest;
import com.crestwood.school_management_system.dto.ApiDtos.ResetPasswordRequest;
import com.crestwood.school_management_system.dto.ApiDtos.ResultResponse;
import com.crestwood.school_management_system.dto.ApiDtos.ResultScoreRequest;
import com.crestwood.school_management_system.repository.EnrollmentApplicationRepository;
import com.crestwood.school_management_system.repository.NotificationRepository;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.SubjectRepository;
import com.crestwood.school_management_system.repository.TeacherRepository;
import com.crestwood.school_management_system.repository.TermRepository;
import com.crestwood.school_management_system.repository.UserRepository;
import com.crestwood.school_management_system.service.AuthService;
import com.crestwood.school_management_system.service.EnrollmentService;
import com.crestwood.school_management_system.service.FeeInvoiceService;
import com.crestwood.school_management_system.service.MailService;
import com.crestwood.school_management_system.service.NoticeboardService;
import com.crestwood.school_management_system.service.ResultService;

@SpringBootTest
@Transactional
class EnrollmentAndResultServiceTests {
	@Autowired
	private EnrollmentService enrollmentService;
	@Autowired
	private AuthService authService;
	@Autowired
	private ResultService resultService;
	@Autowired
	private NoticeboardService noticeboardService;
	@Autowired
	private FeeInvoiceService invoiceService;
	@Autowired
	private EnrollmentApplicationRepository enrollmentRepository;
	@Autowired
	private SchoolClassRepository classRepository;
	@Autowired
	private SubjectRepository subjectRepository;
	@Autowired
	private TeacherRepository teacherRepository;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private TermRepository termRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ParentGuardianRepository parentRepository;
	@Autowired
	private NotificationRepository notificationRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@MockitoBean
	private MailService mailService;

	@Test
	void approvalCreatesLinkedHashedAccountsAndSendsCredentials() {
		SchoolClass schoolClass = classRepository.findByName("JSS 3").orElseThrow();
		Long applicationId = enrollmentService.create(new EnrollmentApplicationRequest("Approval Parent", "approval-parent@example.com", "+2348000000001", "Approval Child", LocalDate.of(2014, 5, 6), "MALE", schoolClass.getId(), null, null)).id();
		User admin = userRepository.findByEmailIgnoreCase("admin@crestwoodacademy.ng").orElseThrow();
		ApprovalResponse response = enrollmentService.review(applicationId, new EnrollmentReviewRequest(EnrollmentStatus.APPROVED), admin);

		assertNotNull(response.studentId());
		assertNotNull(response.temporaryPassword());
		assertEquals(EnrollmentStatus.APPROVED, enrollmentRepository.findById(applicationId).orElseThrow().getStatus());
		User parentUser = userRepository.findByEmailIgnoreCase(response.parentEmail()).orElseThrow();
		User studentUser = userRepository.findByEmailIgnoreCase(response.studentEmail()).orElseThrow();
		assertTrue(passwordEncoder.matches(response.temporaryPassword(), parentUser.getPassword()));
		assertTrue(passwordEncoder.matches(response.temporaryPassword(), studentUser.getPassword()));
		assertNotNull(studentRepository.findById(response.studentId()).orElseThrow());
		verify(mailService).sendEnrollmentApproval(response.parentEmail(), "Approval Child", "Approval Parent", response.studentEmail(), response.parentEmail(), response.temporaryPassword());
	}

	@Test
	void resultTotalsAndGradesAreDerivedOnTheServerAndDeliveryIsTriggered() {
		Teacher teacher = teacherRepository.findByUserEmailIgnoreCase("funmi.adebayo@crestwoodacademy.ng").orElseThrow();
		Student student = studentRepository.findByUserEmailIgnoreCase("tobi.adeyemi@student.crestwoodacademy.ng").orElseThrow();
		var subject = subjectRepository.findByName("Mathematics").orElseThrow();
		Term term = termRepository.findByAcademicSessionIdAndName(termRepository.findFirstByCurrentTrue().orElseThrow().getAcademicSession().getId(), TermName.SECOND_TERM).orElseThrow();
		clearInvocations(mailService);

		ResultResponse result = resultService.save(teacher, new ResultScoreRequest(student.getId(), subject.getId(), term.getId(), 20, 20, 35));

		assertEquals(75, result.total());
		assertEquals("A", result.grade());
		verify(mailService, times(2)).sendResultPosted(anyString(), anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.eq(75), org.mockito.ArgumentMatchers.eq("A"));
	}

	@Test
	void filtersNoticeboardAndNotificationAudienceByRoleAndClass() {
		User admin = userRepository.findByEmailIgnoreCase("admin@crestwoodacademy.ng").orElseThrow();
		User parentUser = userRepository.findByEmailIgnoreCase("folake.adeyemi@crestwoodacademy.ng").orElseThrow();
		Long jss1 = classRepository.findByName("JSS 1").orElseThrow().getId();
		Long jss2 = classRepository.findByName("JSS 2").orElseThrow().getId();
		noticeboardService.create(admin, new NoticeboardCreateRequest("All notice", "All", NoticeboardAudience.ALL, null));
		noticeboardService.create(admin, new NoticeboardCreateRequest("Parent notice", "Parents", NoticeboardAudience.PARENTS, null));
		noticeboardService.create(admin, new NoticeboardCreateRequest("JSS 1 notice", "JSS 1", NoticeboardAudience.SPECIFIC_CLASS, jss1));
		noticeboardService.create(admin, new NoticeboardCreateRequest("JSS 2 notice", "JSS 2", NoticeboardAudience.SPECIFIC_CLASS, jss2));

		var visible = noticeboardService.list(parentUser).stream().map(item -> item.title()).toList();
		assertTrue(visible.contains("All notice"));
		assertTrue(visible.contains("Parent notice"));
		assertTrue(visible.contains("JSS 2 notice"));
		assertTrue(visible.stream().noneMatch(title -> title.equals("JSS 1 notice")));
		assertEquals(3, notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(parentUser.getId()).size());
	}

	@Test
	void exposesPaidAndPendingFeeStatesSeparatelyToParents() {
		var parent = parentRepository.findByUserEmailIgnoreCase("folake.adeyemi@crestwoodacademy.ng").orElseThrow();
		var summaries = invoiceService.listForParent(parent.getId(), null, null);
		assertEquals(2, summaries.size());
		assertEquals(1, summaries.stream().filter(item -> item.status() == PaymentStatus.PAID).count());
		assertEquals(1, summaries.stream().filter(item -> item.status() == PaymentStatus.PENDING).count());
	}

	@Test
	void forgotAndResetPasswordUseGenericResponseAndHashedReplacement() {
		authService.forgotPassword(new ForgotPasswordRequest("admin@crestwoodacademy.ng"));
		ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendPasswordReset(org.mockito.ArgumentMatchers.eq("admin@crestwoodacademy.ng"), token.capture());
		assertEquals("If that email exists in our system, a reset link was sent", authService.genericResetMessage());
		authService.resetPassword(new ResetPasswordRequest(token.getValue(), "new-test-password"));
		User admin = userRepository.findByEmailIgnoreCase("admin@crestwoodacademy.ng").orElseThrow();
		assertTrue(passwordEncoder.matches("new-test-password", admin.getPassword()));
	}
}
