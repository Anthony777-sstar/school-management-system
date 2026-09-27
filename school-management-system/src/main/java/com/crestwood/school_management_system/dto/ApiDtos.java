package com.crestwood.school_management_system.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.crestwood.school_management_system.domain.enums.EnrollmentStatus;
import com.crestwood.school_management_system.domain.enums.NoticeboardAudience;
import com.crestwood.school_management_system.domain.enums.NotificationType;
import com.crestwood.school_management_system.domain.enums.PaymentMethod;
import com.crestwood.school_management_system.domain.enums.PaymentStatus;
import com.crestwood.school_management_system.domain.enums.RosterFlag;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.domain.enums.TermName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ApiDtos {
	private ApiDtos() {
	}

	public record LoginRequest(@NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 100) String password, Role role) {
	}

	public record LoginResponse(String token, String tokenType, long expiresIn, UserResponse user) {
	}

	public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 320) String email) {
	}

	public record ResetPasswordRequest(@NotBlank @Size(max = 120) String token, @NotBlank @Size(min = 8, max = 100) String newPassword) {
	}

	public record ChangePasswordRequest(@NotBlank @Size(max = 100) String currentPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) {
	}

	public record UserResponse(Long id, String email, Role role, boolean enabled, String fullName, Long profileId) {
	}

	public record SchoolClassResponse(Long id, String name, Long classTeacherId, String classTeacherName) {
	}

	public record SubjectResponse(Long id, String name) {
	}

	public record AcademicSessionResponse(Long id, String name, boolean current) {
	}

	public record TermResponse(Long id, Long academicSessionId, String academicSessionName, TermName name, boolean current, LocalDate startDate, LocalDate endDate) {
	}

	public record EnrollmentApplicationRequest(
			@NotBlank @Size(max = 160) String parentFullName,
			@NotBlank @Email @Size(max = 320) String parentEmail,
			@NotBlank @Size(max = 40) String parentPhone,
			@NotBlank @Size(max = 160) String studentFullName,
			@NotNull LocalDate studentDateOfBirth,
			@Size(max = 20) String gender,
			Long applyingForClassId,
			@Size(max = 80) String applyingForClassName,
			@Size(max = 80) String applyingForClass) {
	}

	public record EnrollmentApplicationResponse(
			Long id,
			String parentFullName,
			String parentEmail,
			String parentPhone,
			String studentFullName,
			LocalDate studentDateOfBirth,
			String studentGender,
			Long applyingForClassId,
			String applyingForClassName,
			EnrollmentStatus status,
			Instant submittedAt,
			Instant reviewedAt,
			Long reviewedByUserId) {
	}

	public record EnrollmentReviewRequest(@NotNull EnrollmentStatus status) {
	}

	public record ApprovalResponse(EnrollmentApplicationResponse application, Long studentId, String studentEmail, String parentEmail, String temporaryPassword, String emailDelivery) {
	}

	public record TeacherCreateRequest(
			@NotBlank @Size(max = 160) String fullName,
			@NotBlank @Email @Size(max = 320) String email,
			@NotBlank @Size(max = 40) String phone,
			@Size(min = 8, max = 100) String password,
			List<Long> subjectIds,
			List<Long> schoolClassIds,
			List<Long> classIds,
			Long classTeacherSchoolClassId) {
	}

	public record TeacherUpdateRequest(
			@NotBlank @Size(max = 160) String fullName,
			@NotBlank @Size(max = 40) String phone,
			List<Long> subjectIds,
			List<Long> schoolClassIds,
			List<Long> classIds,
			Long classTeacherSchoolClassId) {
	}

	public record TeacherResponse(
			Long id,
			Long userId,
			String email,
			String fullName,
			String phone,
			boolean enabled,
			List<SubjectResponse> subjects,
			List<SchoolClassResponse> classes,
			Long classTeacherSchoolClassId,
			String temporaryPassword) {
	}

	public record TeacherClassSubjectResponse(Long schoolClassId, String schoolClassName, Long subjectId, String subjectName, long studentCount) {
	}

	public record TeacherSummaryResponse(Long id, String fullName, String subjectName) {
	}

	public record AccountantCreateRequest(
			@NotBlank @Size(max = 160) String fullName,
			@NotBlank @Email @Size(max = 320) String email,
			@NotBlank @Size(max = 40) String phone,
			@Size(min = 8, max = 100) String password) {
	}

	public record AccountantUpdateRequest(@NotBlank @Size(max = 160) String fullName, @NotBlank @Size(max = 40) String phone) {
	}

	public record AccountantResponse(Long id, Long userId, String email, String fullName, String phone, boolean enabled, String temporaryPassword) {
	}

	public record StudentResponse(
			Long id,
			Long userId,
			String email,
			String fullName,
			LocalDate dateOfBirth,
			String gender,
			Long schoolClassId,
			String schoolClassName,
			boolean rosterConfirmed,
			RosterFlag rosterFlag,
			LocalDate admissionDate) {
	}

	public record StudentUpdateRequest(
			@NotBlank @Size(max = 160) String fullName,
			@NotNull LocalDate dateOfBirth,
			@NotBlank @Size(max = 20) String gender,
			@NotNull Long schoolClassId) {
	}

	public record RosterUpdateRequest(Boolean rosterConfirmed, @Size(max = 20) String rosterStatus, @Size(max = 20) String status) {
	}

	public record StudentProfileResponse(
			Long id,
			Long userId,
			String email,
			String fullName,
			LocalDate dateOfBirth,
			String gender,
			Long schoolClassId,
			String schoolClassName,
			boolean rosterConfirmed,
			RosterFlag rosterFlag,
			LocalDate admissionDate,
			Long currentTermId,
			String currentTermName,
			List<TeacherSummaryResponse> teachers) {
	}

	public record ParentChildResponse(Long parentId, String parentName, Long studentId, String studentName, String studentEmail, String schoolClassName) {
	}

	public record TeacherAssignmentRequest(@NotNull Long subjectId, @NotNull Long schoolClassId) {
	}

	public record ResultScoreRequest(
			@NotNull Long studentId,
			Long subjectId,
			Long termId,
			@Min(0) @Max(20) int ca1,
			@Min(0) @Max(20) int ca2,
			@Min(0) @Max(60) int exam) {
	}

	public record ResultBatchRequest(
			Long schoolClassId,
			Long classId,
			Long subjectId,
			Long termId,
			@NotEmpty @Size(max = 500) @Valid List<ResultScoreRequest> results) {
	}

	public record ResultResponse(
			Long id,
			Long studentId,
			String studentName,
			Long subjectId,
			String subjectName,
			Long termId,
			String termName,
			int ca1,
			int ca2,
			int exam,
			int total,
			String grade,
			Long enteredByTeacherId,
			Instant enteredAt) {
	}

	public record StudyMaterialResponse(
			Long id,
			Long teacherId,
			String teacherName,
			Long subjectId,
			String subjectName,
			Long schoolClassId,
			String schoolClassName,
			String title,
			String fileUrl,
			String originalFilename,
			String contentType,
			Long fileSize,
			Instant uploadedAt) {
	}

	public record FeeInvoiceRequest(
			@NotNull Long schoolClassId,
			@NotNull Long termId,
			@NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
			@NotNull LocalDate dueDate) {
	}

	public record FeeInvoiceResponse(
			Long id,
			Long schoolClassId,
			String schoolClassName,
			Long termId,
			String termName,
			BigDecimal amount,
			LocalDate dueDate,
			Long createdByAccountantId,
			Instant createdAt,
			BigDecimal amountPaid,
			BigDecimal outstanding) {
	}

	public record PaymentInitializationRequest(@NotNull Long feeInvoiceId, @NotNull Long studentId) {
	}

	public record PaymentInitializationResponse(
			Long paymentId,
			String reference,
			String authorizationUrl,
			String publicKey,
			BigDecimal amount,
			String currency,
			String email,
			Instant expiresAt) {
	}

	public record PaymentVerifyRequest(
			@NotBlank @Size(max = 120) String reference,
			Long paymentId,
			Long feeInvoiceId,
			Long studentId) {
	}

	public record ManualPaymentRequest(
			@NotNull Long studentId,
			@NotNull Long feeInvoiceId,
			@NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amountPaid,
			@NotNull PaymentMethod paymentMethod) {
	}

	public record PaymentStatusRequest(@NotNull PaymentStatus status) {
	}

	public record PaymentResponse(
			Long id,
			Long studentId,
			String studentName,
			Long feeInvoiceId,
			String className,
			String termName,
			BigDecimal amountPaid,
			PaymentMethod paymentMethod,
			String paystackReference,
			PaymentStatus status,
			Instant paidAt,
			Long recordedByAccountantId,
			Instant createdAt) {
	}

	public record NotificationResponse(Long id, NotificationType type, String title, String message, boolean read, Instant createdAt) {
	}

	public record NoticeboardCreateRequest(
			@NotBlank @Size(max = 200) String title,
			@NotBlank @Size(max = 5000) String body,
			@NotNull NoticeboardAudience audience,
			Long audienceClassId) {
	}

	public record NoticeboardResponse(
			Long id,
			String title,
			String body,
			Long postedByUserId,
			String postedByName,
			NoticeboardAudience audience,
			Long audienceClassId,
			String audienceClassName,
			Instant postedAt) {
	}

	public record SchoolSettingsRequest(
			@NotBlank @Size(max = 200) String schoolName,
			@Size(max = 500) String logoUrl,
			@Size(max = 20) String primaryColor,
			@Size(max = 20) String primaryBrandColor) {
	}

	public record SchoolSettingsResponse(Long id, String schoolName, String logoUrl, String primaryColor, String primaryBrandColor, Instant updatedAt) {
	}

	public record AcademicSessionRequest(@NotBlank @Size(max = 30) String name, boolean current) {
	}

	public record AcademicSessionCurrentRequest(@NotNull Boolean current) {
	}

	public record TermRequest(Long academicSessionId, @NotNull TermName name, boolean current, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {
	}

	public record TermCurrentRequest(@NotNull Boolean current) {
	}

	public record FeeReminderRequest(@NotNull Long studentId, @NotNull Long invoiceId) {
	}

	public record AcademicContextResponse(Long academicSessionId, String academicSessionName, Long termId, String termName, String label) {
	}

	public record PaystackConfigResponse(String publicKey, String currency) {
	}

	public record DashboardStatsResponse(
			long totalStudents,
			long totalTeachers,
			long pendingEnrollments,
			BigDecimal feesCollectedThisTerm,
			BigDecimal totalOutstanding,
			long invoicesCreatedThisTerm,
			long overdueCount,
			long myStudents,
			long awaitingRosterConfirmation,
			long materialsUploaded,
			long resultsPendingEntry) {
	}

	public record StudentDashboardResponse(StudentProfileResponse profile, List<ResultResponse> latestResults, List<NoticeboardResponse> notices) {
	}

	public record ParentDashboardResponse(List<ParentChildResponse> children, List<StudentFeeSummaryResponse> fees, List<ResultResponse> latestResults, List<NoticeboardResponse> notices) {
	}

	public record StudentFeeSummaryResponse(Long studentId, String studentName, Long schoolClassId, String schoolClassName, Long invoiceId, Long termId, String termName, BigDecimal amountInvoiced, BigDecimal amountPaid, BigDecimal amountOwed, PaymentStatus status, LocalDate dueDate) {
	}

	public record OutstandingFeeResponse(Long studentId, String studentName, Long schoolClassId, String schoolClassName, Long termId, String termName, Long invoiceId, BigDecimal invoicedAmount, BigDecimal amountPaid, BigDecimal amountOwed, LocalDate dueDate, long daysOverdue) {
	}

	public record PaymentLedgerResponse(List<PaymentResponse> payments, BigDecimal totalCollected) {
	}

	public record MessageResponse(String message) {
	}
}
