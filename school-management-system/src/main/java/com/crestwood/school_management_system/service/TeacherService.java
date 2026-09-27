package com.crestwood.school_management_system.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.SchoolClass;
import com.crestwood.school_management_system.domain.entity.Subject;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.TeacherSubjectClass;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.dto.ApiDtos.SchoolClassResponse;
import com.crestwood.school_management_system.dto.ApiDtos.SubjectResponse;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherClassSubjectResponse;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherCreateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherResponse;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherUpdateRequest;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ConflictException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.SubjectRepository;
import com.crestwood.school_management_system.repository.TeacherRepository;
import com.crestwood.school_management_system.repository.TeacherSubjectClassRepository;
import com.crestwood.school_management_system.repository.UserRepository;

@Service
public class TeacherService {
	private final UserRepository userRepository;
	private final TeacherRepository teacherRepository;
	private final SchoolClassRepository classRepository;
	private final SubjectRepository subjectRepository;
	private final StudentRepository studentRepository;
	private final TeacherSubjectClassRepository assignmentRepository;
	private final PasswordEncoder passwordEncoder;
	private final CredentialService credentialService;

	public TeacherService(
			UserRepository userRepository,
			TeacherRepository teacherRepository,
			SchoolClassRepository classRepository,
			SubjectRepository subjectRepository,
			StudentRepository studentRepository,
			TeacherSubjectClassRepository assignmentRepository,
			PasswordEncoder passwordEncoder,
			CredentialService credentialService) {
		this.userRepository = userRepository;
		this.teacherRepository = teacherRepository;
		this.classRepository = classRepository;
		this.subjectRepository = subjectRepository;
		this.studentRepository = studentRepository;
		this.assignmentRepository = assignmentRepository;
		this.passwordEncoder = passwordEncoder;
		this.credentialService = credentialService;
	}

	@Transactional(readOnly = true)
	public List<TeacherResponse> list() {
		return teacherRepository.findAllByOrderByFullNameAsc().stream().map(this::response).toList();
	}

	@Transactional
	public TeacherResponse create(TeacherCreateRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("A user with this email already exists");
		}
		String password = request.password() == null || request.password().isBlank() ? credentialService.createTemporaryPassword() : request.password();
		User user = userRepository.save(new User(email, passwordEncoder.encode(password), Role.TEACHER));
		Teacher teacher = teacherRepository.save(new Teacher(user, request.fullName().trim(), request.phone().trim()));
		applyAssignments(teacher, request.subjectIds(), effectiveClassIds(request.schoolClassIds(), request.classIds()));
		if (request.classTeacherSchoolClassId() != null) {
			SchoolClass schoolClass = requireClass(request.classTeacherSchoolClassId());
			schoolClass.setClassTeacher(teacher);
			classRepository.save(schoolClass);
		}
		return response(teacher, password);
	}

	@Transactional
	public TeacherResponse update(Long id, TeacherUpdateRequest request) {
		Teacher teacher = requireTeacher(id);
		teacher.setFullName(request.fullName().trim());
		teacher.setPhone(request.phone().trim());
		teacherRepository.save(teacher);
		List<TeacherSubjectClass> existing = assignmentRepository.findByTeacherId(id);
		assignmentRepository.deleteAll(existing);
		assignmentRepository.flush();
		applyAssignments(teacher, request.subjectIds(), effectiveClassIds(request.schoolClassIds(), request.classIds()));
		classRepository.findByClassTeacherId(id).forEach(schoolClass -> {
			schoolClass.setClassTeacher(null);
			classRepository.save(schoolClass);
		});
		if (request.classTeacherSchoolClassId() != null) {
			SchoolClass schoolClass = requireClass(request.classTeacherSchoolClassId());
			schoolClass.setClassTeacher(teacher);
			classRepository.save(schoolClass);
		}
		return response(teacher);
	}

	@Transactional
	public TeacherResponse addAssignment(Long teacherId, Long subjectId, Long schoolClassId) {
		Teacher teacher = requireTeacher(teacherId);
		Subject subject = requireSubject(subjectId);
		SchoolClass schoolClass = requireClass(schoolClassId);
		if (!assignmentRepository.existsByTeacherIdAndSubjectIdAndSchoolClassId(teacherId, subjectId, schoolClassId)) {
			assignmentRepository.save(new TeacherSubjectClass(teacher, subject, schoolClass));
		}
		return response(teacher);
	}

	@Transactional
	public void removeAssignment(Long teacherId, Long assignmentId) {
		requireTeacher(teacherId);
		TeacherSubjectClass assignment = assignmentRepository.findById(assignmentId).orElseThrow(() -> new NotFoundException("Assignment not found"));
		if (!assignment.getTeacher().getId().equals(teacherId)) {
			throw new ConflictException("Assignment does not belong to this teacher");
		}
		assignmentRepository.delete(assignment);
	}

	@Transactional
	public void disable(Long id) {
		Teacher teacher = requireTeacher(id);
		teacher.getUser().setEnabled(false);
		userRepository.save(teacher.getUser());
	}

	@Transactional(readOnly = true)
	public Teacher requireTeacherForUser(User user) {
		return teacherRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Teacher profile not found"));
	}

	@Transactional(readOnly = true)
	public List<TeacherClassSubjectResponse> classesForTeacher(Teacher teacher) {
		return assignmentRepository.findByTeacherId(teacher.getId()).stream().map(assignment -> new TeacherClassSubjectResponse(assignment.getSchoolClass().getId(), assignment.getSchoolClass().getName(), assignment.getSubject().getId(), assignment.getSubject().getName(), studentRepository.findBySchoolClassIdAndRosterConfirmedOrderByFullNameAsc(assignment.getSchoolClass().getId(), true).size())).toList();
	}

	@Transactional(readOnly = true)
	public boolean canAccessClass(Teacher teacher, Long schoolClassId) {
		return assignmentRepository.existsByTeacherIdAndSchoolClassId(teacher.getId(), schoolClassId) || classRepository.findById(schoolClassId).map(schoolClass -> schoolClass.getClassTeacher() != null && schoolClass.getClassTeacher().getId().equals(teacher.getId())).orElse(false);
	}

	@Transactional(readOnly = true)
	public boolean canTeach(Teacher teacher, Long subjectId, Long schoolClassId) {
		return assignmentRepository.existsByTeacherIdAndSubjectIdAndSchoolClassId(teacher.getId(), subjectId, schoolClassId);
	}

	@Transactional(readOnly = true)
	public TeacherResponse response(Teacher teacher) {
		return response(teacher, null);
	}

	@Transactional(readOnly = true)
	public TeacherResponse response(Teacher teacher, String temporaryPassword) {
		List<TeacherSubjectClass> assignments = assignmentRepository.findByTeacherId(teacher.getId());
		Map<Long, SubjectResponse> subjects = new LinkedHashMap<>();
		Map<Long, SchoolClassResponse> classes = new LinkedHashMap<>();
		for (TeacherSubjectClass assignment : assignments) {
			subjects.putIfAbsent(assignment.getSubject().getId(), new SubjectResponse(assignment.getSubject().getId(), assignment.getSubject().getName()));
			classes.putIfAbsent(assignment.getSchoolClass().getId(), classResponse(assignment.getSchoolClass()));
		}
		Long classTeacherSchoolClassId = classRepository.findByClassTeacherId(teacher.getId()).stream().findFirst().map(SchoolClass::getId).orElse(null);
		return new TeacherResponse(teacher.getId(), teacher.getUser().getId(), teacher.getUser().getEmail(), teacher.getFullName(), teacher.getPhone(), teacher.getUser().isEnabled(), List.copyOf(subjects.values()), List.copyOf(classes.values()), classTeacherSchoolClassId, temporaryPassword);
	}

	@Transactional(readOnly = true)
	public Teacher requireTeacher(Long id) {
		return teacherRepository.findById(id).orElseThrow(() -> new NotFoundException("Teacher not found"));
	}

	private void applyAssignments(Teacher teacher, List<Long> subjectIds, List<Long> classIds) {
		if (subjectIds == null || classIds == null || subjectIds.isEmpty() || classIds.isEmpty()) {
			return;
		}
		for (Long subjectId : new LinkedHashSet<>(subjectIds)) {
			Subject subject = requireSubject(subjectId);
			for (Long classId : new LinkedHashSet<>(classIds)) {
				SchoolClass schoolClass = requireClass(classId);
				if (!assignmentRepository.existsByTeacherIdAndSubjectIdAndSchoolClassId(teacher.getId(), subjectId, classId)) {
					assignmentRepository.save(new TeacherSubjectClass(teacher, subject, schoolClass));
				}
			}
		}
	}

	private SchoolClass requireClass(Long id) {
		return classRepository.findById(id).orElseThrow(() -> new NotFoundException("School class not found"));
	}

	private Subject requireSubject(Long id) {
		return subjectRepository.findById(id).orElseThrow(() -> new NotFoundException("Subject not found"));
	}

	private SchoolClassResponse classResponse(SchoolClass schoolClass) {
		return new SchoolClassResponse(schoolClass.getId(), schoolClass.getName(), schoolClass.getClassTeacher() == null ? null : schoolClass.getClassTeacher().getId(), schoolClass.getClassTeacher() == null ? null : schoolClass.getClassTeacher().getFullName());
	}

	private List<Long> effectiveClassIds(List<Long> schoolClassIds, List<Long> classIds) {
		if (schoolClassIds == null) {
			return classIds;
		}
		if (classIds == null) {
			return schoolClassIds;
		}
		Set<Long> ids = new LinkedHashSet<>(schoolClassIds);
		ids.addAll(classIds);
		return new ArrayList<>(ids);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
