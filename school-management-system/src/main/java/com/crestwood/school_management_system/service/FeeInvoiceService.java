package com.crestwood.school_management_system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.Accountant;
import com.crestwood.school_management_system.domain.entity.FeeInvoice;
import com.crestwood.school_management_system.domain.entity.FeePayment;
import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.SchoolClass;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.Term;
import com.crestwood.school_management_system.domain.enums.NotificationType;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;
import com.crestwood.school_management_system.dto.ApiDtos.FeeInvoiceRequest;
import com.crestwood.school_management_system.dto.ApiDtos.FeeInvoiceResponse;
import com.crestwood.school_management_system.dto.ApiDtos.OutstandingFeeResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentFeeSummaryResponse;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ConflictException;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.FeeInvoiceRepository;
import com.crestwood.school_management_system.repository.FeePaymentRepository;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.TermRepository;

@Service
public class FeeInvoiceService {
	private final FeeInvoiceRepository invoiceRepository;
	private final FeePaymentRepository paymentRepository;
	private final SchoolClassRepository classRepository;
	private final TermRepository termRepository;
	private final StudentRepository studentRepository;
	private final StudentParentLinkRepository linkRepository;
	private final CatalogService catalogService;
	private final NotificationService notificationService;
	private final MailService mailService;

	public FeeInvoiceService(
			FeeInvoiceRepository invoiceRepository,
			FeePaymentRepository paymentRepository,
			SchoolClassRepository classRepository,
			TermRepository termRepository,
			StudentRepository studentRepository,
			StudentParentLinkRepository linkRepository,
			CatalogService catalogService,
			NotificationService notificationService,
			MailService mailService) {
		this.invoiceRepository = invoiceRepository;
		this.paymentRepository = paymentRepository;
		this.classRepository = classRepository;
		this.termRepository = termRepository;
		this.studentRepository = studentRepository;
		this.linkRepository = linkRepository;
		this.catalogService = catalogService;
		this.notificationService = notificationService;
		this.mailService = mailService;
	}

	@Transactional
	public FeeInvoiceResponse create(Accountant accountant, FeeInvoiceRequest request) {
		SchoolClass schoolClass = catalogService.requireClass(request.schoolClassId());
		Term term = catalogService.requireTerm(request.termId());
		if (invoiceRepository.findBySchoolClassIdAndTermId(schoolClass.getId(), term.getId()).isPresent()) {
			throw new ConflictException("An invoice already exists for this class and term");
		}
		FeeInvoice invoice = invoiceRepository.save(new FeeInvoice(schoolClass, term, money(request.amount()), request.dueDate(), accountant));
		for (Student student : studentRepository.findBySchoolClassIdOrderByFullNameAsc(schoolClass.getId())) {
			String amount = invoice.getAmount().toPlainString();
			for (ParentGuardian parent : linkRepository.findParentsByStudentId(student.getId())) {
				mailService.deliver(() -> mailService.sendFeeDue(parent.getUser().getEmail(), student.getFullName(), schoolClass.getName(), term.getName().name(), amount, request.dueDate().toString()));
				notificationService.create(parent.getUser(), NotificationType.FEE_DUE, "School fee due", "The " + term.getName().name() + " fee for " + student.getFullName() + " is due on " + request.dueDate() + ".");
			}
			mailService.deliver(() -> mailService.sendFeeDue(student.getUser().getEmail(), student.getFullName(), schoolClass.getName(), term.getName().name(), amount, request.dueDate().toString()));
			notificationService.create(student.getUser(), NotificationType.FEE_DUE, "School fee due", "The " + term.getName().name() + " fee is due on " + request.dueDate() + ".");
		}
		return response(invoice);
	}

	@Transactional
	public FeeInvoiceResponse update(Accountant accountant, Long id, FeeInvoiceRequest request) {
		FeeInvoice invoice = invoiceRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Fee invoice not found"));
		if (!invoice.getSchoolClass().getId().equals(request.schoolClassId()) || !invoice.getTerm().getId().equals(request.termId())) {
			throw new BadRequestException("An invoice class and term cannot be changed after creation");
		}
		BigDecimal newAmount = money(request.amount());
		BigDecimal amountAlreadyPaid = paidTotal(invoice);
		BigDecimal newTotal = newAmount.multiply(BigDecimal.valueOf(studentRepository.findBySchoolClassIdOrderByFullNameAsc(invoice.getSchoolClass().getId()).size()));
		if (newTotal.compareTo(amountAlreadyPaid) < 0) {
			throw new ConflictException("Invoice amount cannot be lower than payments already recorded");
		}
		invoice.setAmount(newAmount);
		invoice.setDueDate(request.dueDate());
		return response(invoiceRepository.save(invoice));
	}

	@Transactional(readOnly = true)
	public List<FeeInvoiceResponse> listAll(Long termId) {
		List<FeeInvoice> invoices = termId == null ? invoiceRepository.findAllByOrderByCreatedAtDesc() : invoiceRepository.findByTermId(termId);
		return invoices.stream().map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public List<StudentFeeSummaryResponse> listForStudent(Student student, Long termId) {
		return invoiceRepository.findBySchoolClassId(student.getSchoolClass().getId()).stream().filter(invoice -> termId == null || invoice.getTerm().getId().equals(termId)).map(invoice -> summary(invoice, student)).toList();
	}

	@Transactional(readOnly = true)
	public List<StudentFeeSummaryResponse> listForParent(Long parentId, Long studentId, Long termId) {
		List<StudentFeeSummaryResponse> summaries = new ArrayList<>();
		for (var link : linkRepository.findByParentIdOrderByIdAsc(parentId)) {
			if (studentId == null || link.getStudent().getId().equals(studentId)) {
				summaries.addAll(listForStudent(link.getStudent(), termId));
			}
		}
		return summaries;
	}

	@Transactional(readOnly = true)
	public List<OutstandingFeeResponse> outstanding(Long termId, Long schoolClassId) {
		LocalDate today = LocalDate.now();
		List<FeeInvoice> invoices = new ArrayList<>();
		if (termId != null) {
			invoices.addAll(invoiceRepository.findByTermId(termId));
		} else {
			invoices.addAll(invoiceRepository.findAll());
		}
		if (schoolClassId != null) {
			invoices.removeIf(invoice -> !invoice.getSchoolClass().getId().equals(schoolClassId));
		}
		List<OutstandingFeeResponse> rows = new ArrayList<>();
		for (FeeInvoice invoice : invoices) {
			for (Student student : studentRepository.findBySchoolClassIdOrderByFullNameAsc(invoice.getSchoolClass().getId())) {
				BigDecimal paid = paidForInvoiceAndStudent(invoice, student);
				BigDecimal owed = invoice.getAmount().subtract(paid).max(BigDecimal.ZERO);
				if (owed.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				long days = invoice.getDueDate().isBefore(today) ? ChronoUnit.DAYS.between(invoice.getDueDate(), today) : 0;
				rows.add(new OutstandingFeeResponse(student.getId(), student.getFullName(), student.getSchoolClass().getId(), student.getSchoolClass().getName(), invoice.getTerm().getId(), invoice.getTerm().getName().name(), invoice.getId(), invoice.getAmount(), paid, owed, invoice.getDueDate(), days));
			}
		}
		return rows;
	}

	@Transactional(readOnly = true)
	public boolean isMailEnabled() {
		return mailService.isEnabled();
	}

	@Transactional
	public void sendReminder(Long studentId, Long invoiceId) {
		Student student = studentRepository.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
		FeeInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new NotFoundException("Fee invoice not found"));
		if (!invoice.getSchoolClass().getId().equals(student.getSchoolClass().getId())) {
			throw new ForbiddenException("The invoice is not assigned to this student's class");
		}
		BigDecimal amount = outstandingFor(invoice, student);
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BadRequestException("The student has no outstanding balance for this invoice");
		}
		int parentNotifications = 0;
		for (ParentGuardian parent : linkRepository.findParentsByStudentId(studentId)) {
			mailService.deliver(() -> mailService.sendFeeReminder(parent.getUser().getEmail(), student.getFullName(), student.getSchoolClass().getName(), invoice.getTerm().getName().name(), amount.toPlainString(), invoice.getDueDate().toString()));
			notificationService.create(parent.getUser(), NotificationType.FEE_DUE, "School fee reminder", "The fee for " + student.getFullName() + " remains outstanding.");
			parentNotifications++;
		}
		if (parentNotifications == 0) {
			throw new NotFoundException("No parent or guardian is linked to this student for fee reminders");
		}
	}

	@Transactional(readOnly = true)
	public FeeInvoice require(Long id) {
		return invoiceRepository.findById(id).orElseThrow(() -> new NotFoundException("Fee invoice not found"));
	}

	@Transactional(readOnly = true)
	public FeeInvoiceResponse response(FeeInvoice invoice) {
		BigDecimal paid = paidTotal(invoice);
		BigDecimal totalDue = invoice.getAmount().multiply(BigDecimal.valueOf(studentRepository.findBySchoolClassIdOrderByFullNameAsc(invoice.getSchoolClass().getId()).size()));
		return new FeeInvoiceResponse(invoice.getId(), invoice.getSchoolClass().getId(), invoice.getSchoolClass().getName(), invoice.getTerm().getId(), invoice.getTerm().getName().name(), invoice.getAmount(), invoice.getDueDate(), invoice.getCreatedByAccountant().getId(), invoice.getCreatedAt(), paid, totalDue.subtract(paid).max(BigDecimal.ZERO));
	}

	@Transactional(readOnly = true)
	public BigDecimal paidForInvoiceAndStudent(FeeInvoice invoice, Student student) {
		return paymentRepository.findByStudentIdAndFeeInvoiceIdAndStatus(student.getId(), invoice.getId(), PaymentStatus.PAID).stream().map(FeePayment::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Transactional(readOnly = true)
	public BigDecimal outstandingFor(FeeInvoice invoice, Student student) {
		return invoice.getAmount().subtract(paidForInvoiceAndStudent(invoice, student)).max(BigDecimal.ZERO);
	}

	private StudentFeeSummaryResponse summary(FeeInvoice invoice, Student student) {
		BigDecimal paid = paidForInvoiceAndStudent(invoice, student);
		BigDecimal outstanding = outstandingFor(invoice, student);
		PaymentStatus paymentStatus = outstanding.compareTo(BigDecimal.ZERO) == 0 ? PaymentStatus.PAID : PaymentStatus.PENDING;
		return new StudentFeeSummaryResponse(student.getId(), student.getFullName(), student.getSchoolClass().getId(), student.getSchoolClass().getName(), invoice.getId(), invoice.getTerm().getId(), invoice.getTerm().getName().name(), invoice.getAmount(), paid, outstanding, paymentStatus, invoice.getDueDate());
	}

	private BigDecimal paidTotal(FeeInvoice invoice) {
		return paymentRepository.findByFeeInvoiceIdAndStatus(invoice.getId(), PaymentStatus.PAID).stream().map(FeePayment::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal money(BigDecimal amount) {
		try {
			return amount.setScale(2, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException exception) {
			throw new BadRequestException("Payment amount has more than two decimal places");
		}
	}
}
