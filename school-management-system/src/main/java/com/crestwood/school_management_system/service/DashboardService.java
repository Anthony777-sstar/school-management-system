package com.crestwood.school_management_system.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.FeeInvoice;
import com.crestwood.school_management_system.domain.entity.FeePayment;
import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.Term;
import com.crestwood.school_management_system.domain.enums.EnrollmentStatus;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;
import com.crestwood.school_management_system.domain.enums.RosterFlag;
import com.crestwood.school_management_system.dto.ApiDtos.DashboardStatsResponse;
import com.crestwood.school_management_system.dto.ApiDtos.NoticeboardResponse;
import com.crestwood.school_management_system.dto.ApiDtos.ParentDashboardResponse;
import com.crestwood.school_management_system.dto.ApiDtos.ResultResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentDashboardResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentFeeSummaryResponse;
import com.crestwood.school_management_system.repository.EnrollmentApplicationRepository;
import com.crestwood.school_management_system.repository.FeeInvoiceRepository;
import com.crestwood.school_management_system.repository.FeePaymentRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.StudyMaterialRepository;
import com.crestwood.school_management_system.repository.TeacherRepository;
import com.crestwood.school_management_system.repository.TeacherSubjectClassRepository;
import com.crestwood.school_management_system.repository.TermRepository;

@Service
public class DashboardService {
	private final StudentRepository studentRepository;
	private final TeacherRepository teacherRepository;
	private final EnrollmentApplicationRepository enrollmentRepository;
	private final FeeInvoiceRepository invoiceRepository;
	private final FeePaymentRepository paymentRepository;
	private final TermRepository termRepository;
	private final TeacherSubjectClassRepository assignmentRepository;
	private final StudyMaterialRepository materialRepository;
	private final FeeInvoiceService invoiceService;
	private final ResultService resultService;
	private final StudentService studentService;
	private final NoticeboardService noticeboardService;
	private final StudentParentLinkRepository linkRepository;
	private final CatalogService catalogService;

	public DashboardService(
			StudentRepository studentRepository,
			TeacherRepository teacherRepository,
			EnrollmentApplicationRepository enrollmentRepository,
			FeeInvoiceRepository invoiceRepository,
			FeePaymentRepository paymentRepository,
			TermRepository termRepository,
			TeacherSubjectClassRepository assignmentRepository,
			StudyMaterialRepository materialRepository,
			FeeInvoiceService invoiceService,
			ResultService resultService,
			StudentService studentService,
			NoticeboardService noticeboardService,
			StudentParentLinkRepository linkRepository,
			CatalogService catalogService) {
		this.studentRepository = studentRepository;
		this.teacherRepository = teacherRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.invoiceRepository = invoiceRepository;
		this.paymentRepository = paymentRepository;
		this.termRepository = termRepository;
		this.assignmentRepository = assignmentRepository;
		this.materialRepository = materialRepository;
		this.invoiceService = invoiceService;
		this.resultService = resultService;
		this.studentService = studentService;
		this.noticeboardService = noticeboardService;
		this.linkRepository = linkRepository;
		this.catalogService = catalogService;
	}

	@Transactional(readOnly = true)
	public DashboardStatsResponse admin() {
		return financialStats(studentRepository.count(), teacherRepository.count(), enrollmentRepository.countByStatus(EnrollmentStatus.PENDING));
	}

	@Transactional(readOnly = true)
	public DashboardStatsResponse accountant() {
		return financialStats(0, 0, 0);
	}

	@Transactional(readOnly = true)
	public DashboardStatsResponse teacher(Teacher teacher) {
		Set<Long> classIds = new LinkedHashSet<>();
		assignmentRepository.findByTeacherId(teacher.getId()).forEach(assignment -> classIds.add(assignment.getSchoolClass().getId()));
		long myStudents = classIds.stream().mapToLong(classId -> studentRepository.findBySchoolClassIdAndRosterConfirmedOrderByFullNameAsc(classId, true).size()).sum();
		long awaiting = classIds.stream().mapToLong(classId -> studentRepository.findBySchoolClassIdAndRosterConfirmedAndRosterFlagOrderByFullNameAsc(classId, false, RosterFlag.NONE).size()).sum();
		long materials = materialRepository.findByTeacherIdOrderByUploadedAtDesc(teacher.getId()).size();
		Term currentTerm = catalogService.currentTerm();
		long pendingResults = resultService.pendingRows(teacher, currentTerm).stream().filter(row -> row.id() == null).count();
		return emptyTeacherStats(myStudents, awaiting, materials, pendingResults);
	}

	@Transactional(readOnly = true)
	public StudentDashboardResponse student(com.crestwood.school_management_system.domain.entity.User user) {
		Student student = studentService.requireStudentForUser(user);
		List<ResultResponse> results = resultService.forStudent(student.getId(), null);
		List<ResultResponse> latest = results.isEmpty() ? List.of() : List.of(results.get(results.size() - 1));
		List<NoticeboardResponse> notices = noticeboardService.list(user).stream().limit(3).toList();
		return new StudentDashboardResponse(studentService.profile(student), latest, notices);
	}

	@Transactional(readOnly = true)
	public ParentDashboardResponse parent(com.crestwood.school_management_system.domain.entity.User user) {
		ParentGuardian parent = studentService.requireParentForUser(user);
		List<StudentFeeSummaryResponse> fees = invoiceService.listForParent(parent.getId(), null, null);
		List<ResultResponse> latest = new ArrayList<>();
		for (var link : linkRepository.findByParentIdOrderByIdAsc(parent.getId())) {
			List<ResultResponse> results = resultService.forStudent(link.getStudent().getId(), null);
			if (!results.isEmpty()) {
				latest.add(results.get(results.size() - 1));
			}
		}
		return new ParentDashboardResponse(studentService.children(parent), fees, latest, noticeboardService.list(user).stream().limit(3).toList());
	}

	private DashboardStatsResponse financialStats(long students, long teachers, long pendingEnrollments) {
		Term current = termRepository.findFirstByCurrentTrue().orElse(null);
		List<FeeInvoice> invoices = current == null ? List.of() : invoiceRepository.findByTermId(current.getId());
		BigDecimal collected = invoices.stream().flatMap(invoice -> paymentRepository.findByFeeInvoiceIdAndStatus(invoice.getId(), PaymentStatus.PAID).stream()).map(FeePayment::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal outstanding = current == null ? BigDecimal.ZERO : invoiceService.outstanding(current.getId(), null).stream().map(row -> row.amountOwed()).reduce(BigDecimal.ZERO, BigDecimal::add);
		long overdue = current == null ? 0 : invoiceService.outstanding(current.getId(), null).stream().filter(row -> row.daysOverdue() > 0).count();
		return new DashboardStatsResponse(students, teachers, pendingEnrollments, collected, outstanding, invoices.size(), overdue, 0, 0, 0, 0);
	}

	private DashboardStatsResponse emptyTeacherStats(long students, long awaiting, long materials, long pendingResults) {
		return new DashboardStatsResponse(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, students, awaiting, materials, pendingResults);
	}
}
