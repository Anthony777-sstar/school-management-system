package com.crestwood.school_management_system.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.Result;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.Subject;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.TeacherSubjectClass;
import com.crestwood.school_management_system.domain.entity.Term;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.NotificationType;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.dto.ApiDtos.ResultBatchRequest;
import com.crestwood.school_management_system.dto.ApiDtos.ResultResponse;
import com.crestwood.school_management_system.dto.ApiDtos.ResultScoreRequest;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.ResultRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.SubjectRepository;
import com.crestwood.school_management_system.repository.TeacherSubjectClassRepository;
import com.crestwood.school_management_system.repository.TermRepository;

@Service
public class ResultService {
	private final ResultRepository resultRepository;
	private final StudentRepository studentRepository;
	private final SubjectRepository subjectRepository;
	private final TermRepository termRepository;
	private final TeacherSubjectClassRepository assignmentRepository;
	private final TeacherService teacherService;
	private final StudentService studentService;
	private final StudentParentLinkRepository linkRepository;
	private final ParentGuardianRepository parentRepository;
	private final MailService mailService;
	private final NotificationService notificationService;
	private final CatalogService catalogService;

	public ResultService(
			ResultRepository resultRepository,
			StudentRepository studentRepository,
			SubjectRepository subjectRepository,
			TermRepository termRepository,
			TeacherSubjectClassRepository assignmentRepository,
			TeacherService teacherService,
			StudentService studentService,
			StudentParentLinkRepository linkRepository,
			ParentGuardianRepository parentRepository,
			MailService mailService,
			NotificationService notificationService,
			CatalogService catalogService) {
		this.resultRepository = resultRepository;
		this.studentRepository = studentRepository;
		this.subjectRepository = subjectRepository;
		this.termRepository = termRepository;
		this.assignmentRepository = assignmentRepository;
		this.teacherService = teacherService;
		this.studentService = studentService;
		this.linkRepository = linkRepository;
		this.parentRepository = parentRepository;
		this.mailService = mailService;
		this.notificationService = notificationService;
		this.catalogService = catalogService;
	}

	@Transactional
	public List<ResultResponse> saveBatch(Teacher teacher, ResultBatchRequest request) {
		Long defaultClassId = request.schoolClassId() != null ? request.schoolClassId() : request.classId();
		List<ResultResponse> responses = new ArrayList<>();
		Set<String> uniqueScores = new HashSet<>();
		for (ResultScoreRequest score : request.results()) {
			ResolvedScore resolved = resolveScore(teacher, score, defaultClassId, request.subjectId(), request.termId());
			String key = resolved.student().getId() + ":" + resolved.subject().getId() + ":" + resolved.term().getId();
			if (!uniqueScores.add(key)) {
				throw new BadRequestException("A result batch cannot contain the same student, subject and term more than once");
			}
			responses.add(save(teacher, resolved.student(), resolved.subject(), resolved.term(), score));
		}
		return responses;
	}

	@Transactional
	public ResultResponse save(Teacher teacher, ResultScoreRequest score) {
		ResolvedScore resolved = resolveScore(teacher, score, score.studentId() == null ? null : studentRepository.findById(score.studentId()).orElseThrow(() -> new NotFoundException("Student not found")).getSchoolClass().getId(), score.subjectId(), score.termId());
		return save(teacher, resolved.student(), resolved.subject(), resolved.term(), score);
	}

	@Transactional(readOnly = true)
	public List<ResultResponse> listForUser(User user, Long studentId, Long schoolClassId, Long subjectId, Long termId, String status) {
		return switch (user.getRole()) {
			case STUDENT -> {
				Student student = studentService.requireStudentForUser(user);
				if (studentId != null && !studentId.equals(student.getId())) {
					throw new ForbiddenException("You cannot access another student's results");
				}
				yield forStudent(student.getId(), termId);
			}
			case PARENT -> {
				ParentGuardian parent = parentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Parent profile not found"));
				Student student = studentService.requireChild(parent, studentId);
				yield forStudent(student.getId(), termId);
			}
			case TEACHER -> {
				Teacher teacher = teacherService.requireTeacherForUser(user);
				if ("PENDING".equalsIgnoreCase(status) && schoolClassId == null && subjectId == null && termId == null) {
					yield pendingRows(teacher, catalogService.currentTerm());
				}
				if (schoolClassId == null || subjectId == null || termId == null) {
					throw new BadRequestException("Class, subject and term are required for teacher result queries");
				}
				yield entryRows(teacher, schoolClassId, subjectId, termId);
			}
			case SUPER_ADMIN, ACCOUNTANT -> filteredAllResults(studentId, schoolClassId, subjectId, termId);
		};
	}

	@Transactional(readOnly = true)
	public List<ResultResponse> pendingRows(Teacher teacher, Term term) {
		Map<String, ResultResponse> rows = new LinkedHashMap<>();
		for (TeacherSubjectClass assignment : assignmentRepository.findByTeacherId(teacher.getId())) {
			for (ResultResponse row : entryRows(teacher, assignment.getSchoolClass().getId(), assignment.getSubject().getId(), term.getId())) {
				rows.put(row.studentId() + ":" + row.subjectId() + ":" + row.termId(), row);
			}
		}
		return rows.values().stream().filter(row -> row.id() == null).toList();
	}

	@Transactional(readOnly = true)
	public List<ResultResponse> entryRows(Teacher teacher, Long schoolClassId, Long subjectId, Long termId) {
		if (!teacherService.canTeach(teacher, subjectId, schoolClassId)) {
			throw new ForbiddenException("You are not assigned to this class and subject");
		}
		Subject subject = requireSubject(subjectId);
		Term term = requireTerm(termId);
		List<ResultResponse> rows = new ArrayList<>();
		for (Student student : studentRepository.findBySchoolClassIdAndRosterConfirmedOrderByFullNameAsc(schoolClassId, true)) {
			Result result = resultRepository.findByStudentIdAndSubjectIdAndTermId(student.getId(), subjectId, termId).orElse(null);
			rows.add(result == null ? new ResultResponse(null, student.getId(), student.getFullName(), subject.getId(), subject.getName(), term.getId(), term.getName().name(), 0, 0, 0, 0, "F", teacher.getId(), null) : response(result));
		}
		return rows;
	}

	@Transactional(readOnly = true)
	public List<ResultResponse> forStudent(Long studentId, Long termId) {
		List<Result> results = termId == null ? resultRepository.findByStudentIdOrderByIdAsc(studentId) : resultRepository.findByStudentIdAndTermIdOrderBySubjectIdAsc(studentId, termId);
		return results.stream().map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public ResultResponse response(Result result) {
		return new ResultResponse(result.getId(), result.getStudent().getId(), result.getStudent().getFullName(), result.getSubject().getId(), result.getSubject().getName(), result.getTerm().getId(), result.getTerm().getName().name(), result.getCa1(), result.getCa2(), result.getExam(), result.getTotal(), result.getGrade(), result.getEnteredByTeacher().getId(), result.getEnteredAt());
	}

	@Transactional
	public void delete(Long id) {
		resultRepository.delete(requireResult(id));
	}

	@Transactional(readOnly = true)
	public Result requireResult(Long id) {
		return resultRepository.findById(id).orElseThrow(() -> new NotFoundException("Result not found"));
	}

	private ResultResponse save(Teacher teacher, Student student, Subject subject, Term term, ResultScoreRequest score) {
		if (!teacherService.canTeach(teacher, subject.getId(), student.getSchoolClass().getId())) {
			throw new ForbiddenException("You are not assigned to teach this subject and class");
		}
		if (!student.isRosterConfirmed()) {
			throw new ForbiddenException("The student's roster must be confirmed before results can be posted");
		}
		int total = GradeScale.total(score.ca1(), score.ca2(), score.exam());
		String grade = GradeScale.grade(total);
		Result result = resultRepository.findByStudentIdAndSubjectIdAndTermId(student.getId(), subject.getId(), term.getId()).orElse(null);
		if (result == null) {
			result = new Result(student, subject, term, score.ca1(), score.ca2(), score.exam(), total, grade, teacher);
		} else {
			result.updateScores(score.ca1(), score.ca2(), score.exam(), total, grade, teacher);
		}
		Result saved = resultRepository.save(result);
		String message = student.getFullName() + " scored " + total + " (" + grade + ") in " + subject.getName() + " for " + term.getName().name() + ".";
		boolean emailSent = mailService.isEnabled();
		for (ParentGuardian parent : linkRepository.findParentsByStudentId(student.getId())) {
			if (emailSent) {
				emailSent = mailService.deliver(() -> mailService.sendResultPosted(parent.getUser().getEmail(), student.getFullName(), subject.getName(), term.getName().name(), total, grade));
			}
			notificationService.create(parent.getUser(), NotificationType.RESULT_POSTED, "New result posted", message);
		}
		if (emailSent) {
			emailSent = mailService.deliver(() -> mailService.sendResultPosted(student.getUser().getEmail(), student.getFullName(), subject.getName(), term.getName().name(), total, grade));
		}
		notificationService.create(student.getUser(), NotificationType.RESULT_POSTED, "New result posted", message);
		return response(saved);
	}

	private ResolvedScore resolveScore(Teacher teacher, ResultScoreRequest score, Long defaultClassId, Long defaultSubjectId, Long defaultTermId) {
		Student student = studentRepository.findById(score.studentId()).orElseThrow(() -> new NotFoundException("Student not found"));
		Long classId = defaultClassId == null ? student.getSchoolClass().getId() : defaultClassId;
		if (!classId.equals(student.getSchoolClass().getId())) {
			throw new BadRequestException("The selected class does not match the student's class");
		}
		Subject subject = resolveSubject(teacher, student, score.subjectId(), defaultSubjectId);
		Term term = resolveTerm(score.termId(), defaultTermId, student, subject);
		return new ResolvedScore(student, subject, term);
	}

	private Subject resolveSubject(Teacher teacher, Student student, Long requestedSubjectId, Long defaultSubjectId) {
		Long subjectId = requestedSubjectId != null ? requestedSubjectId : defaultSubjectId;
		if (subjectId == null) {
			List<Long> existingSubjectIds = resultRepository.findByStudentIdOrderByIdAsc(student.getId()).stream().map(result -> result.getSubject().getId()).distinct().toList();
			if (existingSubjectIds.size() == 1) {
				subjectId = existingSubjectIds.get(0);
			} else {
				List<Long> assignedSubjectIds = assignmentRepository.findByTeacherIdAndSchoolClassId(teacher.getId(), student.getSchoolClass().getId()).stream().map(assignment -> assignment.getSubject().getId()).distinct().toList();
				if (assignedSubjectIds.size() != 1) {
					throw new BadRequestException("Subject is required when the teacher has multiple assignments for the class");
				}
				subjectId = assignedSubjectIds.get(0);
			}
		}
		return requireSubject(subjectId);
	}

	private Term resolveTerm(Long requestedTermId, Long defaultTermId, Student student, Subject subject) {
		Long termId = requestedTermId != null ? requestedTermId : defaultTermId;
		if (termId == null) {
			List<Long> existingTermIds = resultRepository.findByStudentIdOrderByIdAsc(student.getId()).stream().filter(result -> result.getSubject().getId().equals(subject.getId())).map(result -> result.getTerm().getId()).distinct().toList();
			termId = existingTermIds.size() == 1 ? existingTermIds.get(0) : catalogService.currentTerm().getId();
		}
		return requireTerm(termId);
	}

	private List<ResultResponse> filteredAllResults(Long studentId, Long schoolClassId, Long subjectId, Long termId) {
		return resultRepository.findAll().stream()
				.filter(result -> studentId == null || result.getStudent().getId().equals(studentId))
				.filter(result -> schoolClassId == null || result.getStudent().getSchoolClass().getId().equals(schoolClassId))
				.filter(result -> subjectId == null || result.getSubject().getId().equals(subjectId))
				.filter(result -> termId == null || result.getTerm().getId().equals(termId))
				.sorted((left, right) -> right.getEnteredAt().compareTo(left.getEnteredAt()))
				.map(this::response)
				.toList();
	}

	private Subject requireSubject(Long id) {
		return subjectRepository.findById(id).orElseThrow(() -> new NotFoundException("Subject not found"));
	}

	private Term requireTerm(Long id) {
		return termRepository.findById(id).orElseThrow(() -> new NotFoundException("Term not found"));
	}

	private record ResolvedScore(Student student, Subject subject, Term term) {
	}
}
