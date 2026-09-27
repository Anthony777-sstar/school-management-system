package com.crestwood.school_management_system.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.EnrollmentApplication;
import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.SchoolClass;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.StudentParentLink;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.EnrollmentStatus;
import com.crestwood.school_management_system.domain.enums.NotificationType;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.dto.ApiDtos.ApprovalResponse;
import com.crestwood.school_management_system.dto.ApiDtos.EnrollmentApplicationRequest;
import com.crestwood.school_management_system.dto.ApiDtos.EnrollmentApplicationResponse;
import com.crestwood.school_management_system.dto.ApiDtos.EnrollmentReviewRequest;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ConflictException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.EnrollmentApplicationRepository;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.UserRepository;

@Service
public class EnrollmentService {
	private final EnrollmentApplicationRepository applicationRepository;
	private final SchoolClassRepository classRepository;
	private final UserRepository userRepository;
	private final ParentGuardianRepository parentRepository;
	private final StudentRepository studentRepository;
	private final StudentParentLinkRepository linkRepository;
	private final PasswordEncoder passwordEncoder;
	private final MailService mailService;
	private final NotificationService notificationService;
	private final CredentialService credentialService;

	public EnrollmentService(
			EnrollmentApplicationRepository applicationRepository,
			SchoolClassRepository classRepository,
			UserRepository userRepository,
			ParentGuardianRepository parentRepository,
			StudentRepository studentRepository,
			StudentParentLinkRepository linkRepository,
			PasswordEncoder passwordEncoder,
			MailService mailService,
			NotificationService notificationService,
			CredentialService credentialService) {
		this.applicationRepository = applicationRepository;
		this.classRepository = classRepository;
		this.userRepository = userRepository;
		this.parentRepository = parentRepository;
		this.studentRepository = studentRepository;
		this.linkRepository = linkRepository;
		this.passwordEncoder = passwordEncoder;
		this.mailService = mailService;
		this.notificationService = notificationService;
		this.credentialService = credentialService;
	}

	@Transactional
	public EnrollmentApplicationResponse create(EnrollmentApplicationRequest request) {
		SchoolClass schoolClass = resolveClass(request.applyingForClassId(), firstNonBlank(request.applyingForClassName(), request.applyingForClass()));
		if (request.studentDateOfBirth().isAfter(LocalDate.now())) {
			throw new BadRequestException("Student date of birth cannot be in the future");
		}
		String parentEmail = normalizeEmail(request.parentEmail());
		String studentName = request.studentFullName().trim();
		EnrollmentApplication existing = applicationRepository.findFirstByParentEmailIgnoreCaseAndStudentFullNameIgnoreCaseAndApplyingForClassIdAndStatus(parentEmail, studentName, schoolClass.getId(), EnrollmentStatus.PENDING).orElse(null);
		if (existing != null) {
			return response(existing);
		}
		EnrollmentApplication application = new EnrollmentApplication(request.parentFullName().trim(), parentEmail, request.parentPhone().trim(), studentName, request.studentDateOfBirth(), request.gender(), schoolClass);
		return response(applicationRepository.save(application));
	}

	@Transactional(readOnly = true)
	public List<EnrollmentApplicationResponse> list(EnrollmentStatus status) {
		List<EnrollmentApplication> applications = status == null ? applicationRepository.findAllByOrderBySubmittedAtDesc() : applicationRepository.findByStatusOrderBySubmittedAtDesc(status);
		return applications.stream().map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public EnrollmentApplicationResponse get(Long id) {
		return response(require(id));
	}

	@Transactional
	public ApprovalResponse review(Long id, EnrollmentReviewRequest request, User reviewer) {
		EnrollmentApplication application = require(id);
		if (application.getStatus() != EnrollmentStatus.PENDING) {
			throw new ConflictException("Only pending enrollment applications can be reviewed");
		}
		if (request.status() == EnrollmentStatus.PENDING) {
			throw new BadRequestException("Review status must be APPROVED or REJECTED");
		}
		if (request.status() == EnrollmentStatus.REJECTED) {
			application.setStatus(EnrollmentStatus.REJECTED);
			application.setReviewedAt(java.time.Instant.now());
			application.setReviewedByUser(reviewer);
			return new ApprovalResponse(response(applicationRepository.save(application)), null, null, application.getParentEmail(), null, "No email is sent for a rejected application.");
		}
		String parentEmail = normalizeEmail(application.getParentEmail());
		if (userRepository.existsByEmailIgnoreCase(parentEmail)) {
			throw new ConflictException("The parent email is already registered");
		}
		String temporaryPassword = credentialService.createTemporaryPassword();
		User parentUser = userRepository.save(new User(parentEmail, passwordEncoder.encode(temporaryPassword), Role.PARENT));
		ParentGuardian parent = parentRepository.save(new ParentGuardian(parentUser, application.getParentFullName(), application.getParentPhone()));
		String studentEmail = uniqueStudentEmail(application.getStudentFullName());
		User studentUser = userRepository.save(new User(studentEmail, passwordEncoder.encode(temporaryPassword), Role.STUDENT));
		Student student = studentRepository.save(new Student(studentUser, application.getStudentFullName(), application.getStudentDateOfBirth(), application.getStudentGender(), application.getApplyingForClass(), LocalDate.now()));
		linkRepository.save(new StudentParentLink(parent, student));
		application.setStatus(EnrollmentStatus.APPROVED);
		application.setReviewedAt(java.time.Instant.now());
		application.setReviewedByUser(reviewer);
		applicationRepository.save(application);
		notificationService.create(parentUser, NotificationType.ENROLLMENT_APPROVED, "Enrollment approved", "The enrollment application for " + student.getFullName() + " was approved.");
		String credentialNotice = "Your account is ready. Share the temporary password from your admission confirmation with the portal administrator.";
		notificationService.create(studentUser, NotificationType.ENROLLMENT_APPROVED, "Welcome to Crestwood Academy", credentialNotice);
		boolean emailSent = mailService.deliver(() -> mailService.sendEnrollmentApproval(parentEmail, student.getFullName(), parent.getFullName(), studentEmail, parentEmail, temporaryPassword));
		String emailDelivery = emailSent
				? "Enrollment approval email delivered to the parent guardian."
				: "Email delivery is disabled, so the approval email was not sent. The temporary password is returned once in this response and stored only as a BCrypt hash.";
		return new ApprovalResponse(response(application), student.getId(), studentEmail, parentEmail, temporaryPassword, emailDelivery);
	}

	public EnrollmentApplicationResponse response(EnrollmentApplication application) {
		return new EnrollmentApplicationResponse(application.getId(), application.getParentFullName(), application.getParentEmail(), application.getParentPhone(), application.getStudentFullName(), application.getStudentDateOfBirth(), application.getStudentGender(), application.getApplyingForClass().getId(), application.getApplyingForClass().getName(), application.getStatus(), application.getSubmittedAt(), application.getReviewedAt(), application.getReviewedByUser() == null ? null : application.getReviewedByUser().getId());
	}

	private EnrollmentApplication require(Long id) {
		return applicationRepository.findById(id).orElseThrow(() -> new NotFoundException("Enrollment application not found"));
	}

	private SchoolClass resolveClass(Long id, String name) {
		if (id != null) {
			return classRepository.findById(id).orElseThrow(() -> new NotFoundException("School class not found"));
		}
		if (name == null || name.isBlank()) {
			throw new BadRequestException("An application class is required");
		}
		return classRepository.findByName(name.trim()).orElseThrow(() -> new NotFoundException("School class not found"));
	}

	private String uniqueStudentEmail(String fullName) {
		String base = fullName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".").replaceAll("(^\\.|\\.$)", "");
		if (base.isBlank()) {
			base = "student";
		}
		String candidate = base + "@student.crestwoodacademy.ng";
		int suffix = 1;
		while (userRepository.existsByEmailIgnoreCase(candidate)) {
			candidate = base + "+" + suffix + "@student.crestwoodacademy.ng";
			suffix++;
		}
		return candidate;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

	private String firstNonBlank(String first, String second) {
		return first != null && !first.isBlank() ? first : second;
	}
}
