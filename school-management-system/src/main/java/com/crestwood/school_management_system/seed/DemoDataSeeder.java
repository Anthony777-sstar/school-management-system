package com.crestwood.school_management_system.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.AcademicSession;
import com.crestwood.school_management_system.domain.entity.Accountant;
import com.crestwood.school_management_system.domain.entity.EnrollmentApplication;
import com.crestwood.school_management_system.domain.entity.FeeInvoice;
import com.crestwood.school_management_system.domain.entity.FeePayment;
import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.Result;
import com.crestwood.school_management_system.domain.entity.SchoolClass;
import com.crestwood.school_management_system.domain.entity.SchoolSettings;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.StudentParentLink;
import com.crestwood.school_management_system.domain.entity.Subject;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.TeacherSubjectClass;
import com.crestwood.school_management_system.domain.entity.Term;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.EnrollmentStatus;
import com.crestwood.school_management_system.domain.enums.PaymentMethod;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.domain.enums.TermName;
import com.crestwood.school_management_system.repository.AcademicSessionRepository;
import com.crestwood.school_management_system.repository.AccountantRepository;
import com.crestwood.school_management_system.repository.EnrollmentApplicationRepository;
import com.crestwood.school_management_system.repository.FeeInvoiceRepository;
import com.crestwood.school_management_system.repository.FeePaymentRepository;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.ResultRepository;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.SchoolSettingsRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.SubjectRepository;
import com.crestwood.school_management_system.repository.TeacherRepository;
import com.crestwood.school_management_system.repository.TeacherSubjectClassRepository;
import com.crestwood.school_management_system.repository.TermRepository;
import com.crestwood.school_management_system.repository.UserRepository;
import com.crestwood.school_management_system.service.GradeScale;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
	private final UserRepository userRepository;
	private final TeacherRepository teacherRepository;
	private final StudentRepository studentRepository;
	private final ParentGuardianRepository parentRepository;
	private final AccountantRepository accountantRepository;
	private final SchoolClassRepository classRepository;
	private final SubjectRepository subjectRepository;
	private final TeacherSubjectClassRepository assignmentRepository;
	private final AcademicSessionRepository sessionRepository;
	private final TermRepository termRepository;
	private final EnrollmentApplicationRepository enrollmentRepository;
	private final ResultRepository resultRepository;
	private final FeeInvoiceRepository invoiceRepository;
	private final FeePaymentRepository paymentRepository;
	private final StudentParentLinkRepository linkRepository;
	private final SchoolSettingsRepository settingsRepository;
	private final PasswordEncoder passwordEncoder;
	private final String seedPassword;

	public DemoDataSeeder(
			UserRepository userRepository,
			TeacherRepository teacherRepository,
			StudentRepository studentRepository,
			ParentGuardianRepository parentRepository,
			AccountantRepository accountantRepository,
			SchoolClassRepository classRepository,
			SubjectRepository subjectRepository,
			TeacherSubjectClassRepository assignmentRepository,
			AcademicSessionRepository sessionRepository,
			TermRepository termRepository,
			EnrollmentApplicationRepository enrollmentRepository,
			ResultRepository resultRepository,
			FeeInvoiceRepository invoiceRepository,
			FeePaymentRepository paymentRepository,
			StudentParentLinkRepository linkRepository,
			SchoolSettingsRepository settingsRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.seed.password}") String seedPassword) {
		this.userRepository = userRepository;
		this.teacherRepository = teacherRepository;
		this.studentRepository = studentRepository;
		this.parentRepository = parentRepository;
		this.accountantRepository = accountantRepository;
		this.classRepository = classRepository;
		this.subjectRepository = subjectRepository;
		this.assignmentRepository = assignmentRepository;
		this.sessionRepository = sessionRepository;
		this.termRepository = termRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.resultRepository = resultRepository;
		this.invoiceRepository = invoiceRepository;
		this.paymentRepository = paymentRepository;
		this.linkRepository = linkRepository;
		this.settingsRepository = settingsRepository;
		this.passwordEncoder = passwordEncoder;
		this.seedPassword = seedPassword;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		Map<String, SchoolClass> classes = seedClasses();
		Map<String, Subject> subjects = seedSubjects();
		AcademicSession session = seedSession();
		Map<TermName, Term> terms = seedTerms(session);
		seedSettings();
		User admin = user("admin@crestwoodacademy.ng", Role.SUPER_ADMIN);
		Teacher adebayo = teacher("funmi.adebayo@crestwoodacademy.ng", "Mrs. Adebayo Funmi", "+2348020000001", admin);
		Teacher chukwu = teacher("emeka.chukwu@crestwoodacademy.ng", "Mr. Chukwu Emeka", "+2348020000002", admin);
		Teacher okoro = teacher("ifeoma.okoro@crestwoodacademy.ng", "Ms. Okoro Ifeoma", "+2348020000003", admin);
		assignment(adebayo, subjects.get("Mathematics"), classes.get("JSS 2"));
		assignment(chukwu, subjects.get("English Language"), classes.get("SS 1"));
		assignment(okoro, subjects.get("Biology"), classes.get("SS 2"));
		assignment(okoro, subjects.get("Chemistry"), classes.get("SS 2"));
		classes.get("JSS 2").setClassTeacher(adebayo);
		classes.get("SS 1").setClassTeacher(chukwu);
		classes.get("SS 2").setClassTeacher(okoro);
		classRepository.saveAll(List.of(classes.get("JSS 2"), classes.get("SS 1"), classes.get("SS 2")));
		Student tobi = student("tobi.adeyemi@student.crestwoodacademy.ng", "Tobi Adeyemi", LocalDate.of(2013, 3, 14), "MALE", classes.get("JSS 2"), true);
		Student halima = student("halima.suleiman@student.crestwoodacademy.ng", "Halima Suleiman", LocalDate.of(2013, 7, 2), "FEMALE", classes.get("JSS 2"), true);
		ParentGuardian tobiParent = parent("folake.adeyemi@crestwoodacademy.ng", "Mrs. Folake Adeyemi", "+2348110000001", admin);
		ParentGuardian halimaParent = parent("suleiman.bello@crestwoodacademy.ng", "Mr. Suleiman Bello", "+2348110000002", admin);
		link(tobiParent, tobi);
		link(halimaParent, halima);
		accountant("grace.okafor@crestwoodacademy.ng", "Mrs. Grace Okafor", "+2348330000001", admin);
		resultIfMissing(tobi, subjects.get("Mathematics"), terms.get(TermName.SECOND_TERM), 16, 17, 48, adebayo);
		resultIfMissing(halima, subjects.get("Mathematics"), terms.get(TermName.SECOND_TERM), 14, 15, 40, adebayo);
		FeeInvoice firstTerm = invoiceIfMissing(classes.get("JSS 2"), terms.get(TermName.FIRST_TERM), new BigDecimal("185000.00"), LocalDate.of(2026, 1, 30), admin);
		FeeInvoice secondTerm = invoiceIfMissing(classes.get("JSS 2"), terms.get(TermName.SECOND_TERM), new BigDecimal("185000.00"), LocalDate.of(2026, 9, 30), admin);
		paymentIfMissing(tobi, firstTerm, new BigDecimal("185000.00"), PaymentMethod.PAYSTACK, "seed-tobi-first-term-2025-2026", PaymentStatus.PAID, Instant.parse("2026-01-20T10:00:00Z"));
		paymentIfMissing(tobi, secondTerm, new BigDecimal("185000.00"), PaymentMethod.PAYSTACK, null, PaymentStatus.PENDING, null);
		seedPendingEnrollment(classes.get("JSS 1"), admin);
	}

	private Map<String, SchoolClass> seedClasses() {
		Map<String, SchoolClass> classes = new LinkedHashMap<>();
		for (String name : List.of("JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3")) {
			classes.put(name, classRepository.findByName(name).orElseGet(() -> classRepository.save(new SchoolClass(name))));
		}
		return classes;
	}

	private Map<String, Subject> seedSubjects() {
		Map<String, Subject> subjects = new LinkedHashMap<>();
		for (String name : List.of("Mathematics", "English Language", "Basic Science", "Biology", "Chemistry", "Physics", "Civic Education", "Social Studies", "Agricultural Science", "Computer Studies")) {
			subjects.put(name, subjectRepository.findByName(name).orElseGet(() -> subjectRepository.save(new Subject(name))));
		}
		return subjects;
	}

	private AcademicSession seedSession() {
		AcademicSession session = sessionRepository.findByName("2025/2026").orElseGet(() -> new AcademicSession("2025/2026", true));
		session.setCurrent(true);
		sessionRepository.findAll().stream().filter(existing -> !existing.getName().equals("2025/2026")).forEach(existing -> existing.setCurrent(false));
		return sessionRepository.save(session);
	}

	private Map<TermName, Term> seedTerms(AcademicSession session) {
		Map<TermName, Term> terms = new LinkedHashMap<>();
		terms.put(TermName.FIRST_TERM, term(session, TermName.FIRST_TERM, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 19), false));
		terms.put(TermName.SECOND_TERM, term(session, TermName.SECOND_TERM, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 4, 11), true));
		terms.put(TermName.THIRD_TERM, term(session, TermName.THIRD_TERM, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 7, 25), false));
		termRepository.findAll().stream().filter(existing -> !existing.getAcademicSession().getName().equals("2025/2026")).forEach(existing -> existing.setCurrent(false));
		return terms;
	}

	private Term term(AcademicSession session, TermName name, LocalDate start, LocalDate end, boolean current) {
		Term term = termRepository.findByAcademicSessionIdAndName(session.getId(), name).orElseGet(() -> new Term(session, name, current, start, end));
		term.setCurrent(current);
		return termRepository.save(term);
	}

	private void seedSettings() {
		if (settingsRepository.findFirstByOrderByIdAsc().isEmpty()) {
			settingsRepository.save(new SchoolSettings("Crestwood Academy", "", "#180D38"));
		}
	}

	private User user(String email, Role role) {
		String normalized = email.toLowerCase();
		return userRepository.findByEmailIgnoreCase(normalized).map(existing -> {
			if (existing.getRole() != role) {
				throw new IllegalStateException("Seed account " + normalized + " has an unexpected role");
			}
			return existing;
		}).orElseGet(() -> userRepository.save(new User(normalized, passwordEncoder.encode(seedPassword), role)));
	}

	private Teacher teacher(String email, String fullName, String phone, User admin) {
		User user = user(email, Role.TEACHER);
		user.setEnabled(true);
		userRepository.save(user);
		return teacherRepository.findByUserId(user.getId()).orElseGet(() -> teacherRepository.save(new Teacher(user, fullName, phone)));
	}

	private Student student(String email, String fullName, LocalDate dateOfBirth, String gender, SchoolClass schoolClass, boolean rosterConfirmed) {
		User user = user(email, Role.STUDENT);
		user.setEnabled(true);
		userRepository.save(user);
		Student student = studentRepository.findByUserId(user.getId()).orElseGet(() -> new Student(user, fullName, dateOfBirth, gender, schoolClass, LocalDate.of(2025, 9, 1)));
		student.setSchoolClass(schoolClass);
		if (rosterConfirmed) {
			student.confirmRoster();
		} else {
			student.clearRoster();
		}
		return studentRepository.save(student);
	}

	private ParentGuardian parent(String email, String fullName, String phone, User admin) {
		User user = user(email, Role.PARENT);
		user.setEnabled(true);
		userRepository.save(user);
		return parentRepository.findByUserId(user.getId()).orElseGet(() -> parentRepository.save(new ParentGuardian(user, fullName, phone)));
	}

	private Accountant accountant(String email, String fullName, String phone, User admin) {
		User user = user(email, Role.ACCOUNTANT);
		user.setEnabled(true);
		userRepository.save(user);
		return accountantRepository.findByUserId(user.getId()).orElseGet(() -> accountantRepository.save(new Accountant(user, fullName, phone)));
	}

	private void assignment(Teacher teacher, Subject subject, SchoolClass schoolClass) {
		if (!assignmentRepository.existsByTeacherIdAndSubjectIdAndSchoolClassId(teacher.getId(), subject.getId(), schoolClass.getId())) {
			assignmentRepository.save(new TeacherSubjectClass(teacher, subject, schoolClass));
		}
	}

	private void link(ParentGuardian parent, Student student) {
		if (!linkRepository.existsByParentIdAndStudentId(parent.getId(), student.getId())) {
			linkRepository.save(new StudentParentLink(parent, student));
		}
	}

	private void resultIfMissing(Student student, Subject subject, Term term, int ca1, int ca2, int exam, Teacher teacher) {
		if (resultRepository.findByStudentIdAndSubjectIdAndTermId(student.getId(), subject.getId(), term.getId()).isEmpty()) {
			int total = GradeScale.total(ca1, ca2, exam);
			resultRepository.save(new Result(student, subject, term, ca1, ca2, exam, total, GradeScale.grade(total), teacher));
		}
	}

	private FeeInvoice invoiceIfMissing(SchoolClass schoolClass, Term term, BigDecimal amount, LocalDate dueDate, User admin) {
		return invoiceRepository.findBySchoolClassIdAndTermId(schoolClass.getId(), term.getId()).orElseGet(() -> invoiceRepository.save(new FeeInvoice(schoolClass, term, amount, dueDate, accountantForUser(admin))));
	}

	private Accountant accountantForUser(User admin) {
		return accountantRepository.findByUserEmailIgnoreCase("grace.okafor@crestwoodacademy.ng").orElseThrow(() -> new IllegalStateException("Seed accountant profile is missing"));
	}

	private void paymentIfMissing(Student student, FeeInvoice invoice, BigDecimal amount, PaymentMethod method, String reference, PaymentStatus status, Instant paidAt) {
		if (!paymentRepository.existsByStudentIdAndFeeInvoiceIdAndStatusAndPaymentMethod(student.getId(), invoice.getId(), status, method)) {
			FeePayment payment = new FeePayment(student, invoice, amount, method, reference, status, accountantForUser(userRepository.findByEmailIgnoreCase("admin@crestwoodacademy.ng").orElseThrow()));
			payment.setPaidAt(paidAt);
			paymentRepository.save(payment);
		}
	}

	private void seedPendingEnrollment(SchoolClass schoolClass, User admin) {
		if (enrollmentRepository.findFirstByParentEmailIgnoreCaseAndStudentFullNameIgnoreCaseAndApplyingForClassIdAndStatus("chidinma.okoro@example.com", "Emeka Okoro", schoolClass.getId(), EnrollmentStatus.PENDING).isEmpty()) {
			enrollmentRepository.save(new EnrollmentApplication("Mrs. Chidinma Okoro", "chidinma.okoro@example.com", "+2348220000001", "Emeka Okoro", LocalDate.of(2016, 5, 9), "MALE", schoolClass));
		}
	}
}
