package com.crestwood.school_management_system.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.SchoolClass;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.Term;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.RosterFlag;
import com.crestwood.school_management_system.dto.ApiDtos.ParentChildResponse;
import com.crestwood.school_management_system.dto.ApiDtos.RosterUpdateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.StudentProfileResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentResponse;
import com.crestwood.school_management_system.dto.ApiDtos.StudentUpdateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.TeacherSummaryResponse;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.TeacherSubjectClassRepository;

@Service
public class StudentService {
	private final StudentRepository studentRepository;
	private final SchoolClassRepository classRepository;
	private final ParentGuardianRepository parentRepository;
	private final StudentParentLinkRepository linkRepository;
	private final TeacherSubjectClassRepository assignmentRepository;
	private final TeacherService teacherService;
	private final CatalogService catalogService;

	public StudentService(
			StudentRepository studentRepository,
			SchoolClassRepository classRepository,
			ParentGuardianRepository parentRepository,
			StudentParentLinkRepository linkRepository,
			TeacherSubjectClassRepository assignmentRepository,
			TeacherService teacherService,
			CatalogService catalogService) {
		this.studentRepository = studentRepository;
		this.classRepository = classRepository;
		this.parentRepository = parentRepository;
		this.linkRepository = linkRepository;
		this.assignmentRepository = assignmentRepository;
		this.teacherService = teacherService;
		this.catalogService = catalogService;
	}

	@Transactional(readOnly = true)
	public List<StudentResponse> list(Long schoolClassId, String className) {
		SchoolClass schoolClass = resolveOptionalClass(schoolClassId, className);
		List<Student> students = schoolClass == null ? studentRepository.findAllByOrderByFullNameAsc() : studentRepository.findBySchoolClassIdOrderByFullNameAsc(schoolClass.getId());
		return students.stream().map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public List<StudentResponse> awaitingRoster(Teacher teacher, Long schoolClassId, String rosterStatus) {
		if (schoolClassId != null && !teacherService.canAccessClass(teacher, schoolClassId)) {
			throw new ForbiddenException("You are not assigned to this class");
		}
		List<Student> students = new ArrayList<>();
		if (rosterStatus == null || rosterStatus.equalsIgnoreCase("PENDING")) {
			if (schoolClassId == null) {
				assignmentRepository.findByTeacherId(teacher.getId()).stream().map(assignment -> assignment.getSchoolClass().getId()).distinct().forEach(classId -> students.addAll(studentRepository.findBySchoolClassIdAndRosterConfirmedAndRosterFlagOrderByFullNameAsc(classId, false, RosterFlag.NONE)));
			} else {
				students.addAll(studentRepository.findBySchoolClassIdAndRosterConfirmedAndRosterFlagOrderByFullNameAsc(schoolClassId, false, RosterFlag.NONE));
			}
		} else if (rosterStatus.equalsIgnoreCase("FLAGGED")) {
			if (schoolClassId == null) {
				assignmentRepository.findByTeacherId(teacher.getId()).stream().map(assignment -> assignment.getSchoolClass().getId()).distinct().forEach(classId -> students.addAll(studentRepository.findBySchoolClassIdAndRosterConfirmedAndRosterFlagOrderByFullNameAsc(classId, false, RosterFlag.FLAGGED)));
			} else {
				students.addAll(studentRepository.findBySchoolClassIdAndRosterConfirmedAndRosterFlagOrderByFullNameAsc(schoolClassId, false, RosterFlag.FLAGGED));
			}
		} else if (!rosterStatus.equalsIgnoreCase("ALL")) {
			throw new BadRequestException("Roster status must be PENDING, FLAGGED or ALL");
		} else if (schoolClassId == null) {
			assignmentRepository.findByTeacherId(teacher.getId()).stream().map(assignment -> assignment.getSchoolClass().getId()).distinct().forEach(classId -> students.addAll(studentRepository.findBySchoolClassIdOrderByFullNameAsc(classId)));
		} else {
			students.addAll(studentRepository.findBySchoolClassIdOrderByFullNameAsc(schoolClassId));
		}
		Map<Long, Student> unique = new LinkedHashMap<>();
		students.forEach(student -> unique.put(student.getId(), student));
		return unique.values().stream().map(this::response).toList();
	}

	@Transactional
	public StudentResponse updateRoster(Teacher teacher, Long studentId, RosterUpdateRequest request) {
		Student student = requireStudent(studentId);
		requireClassAccess(teacher, student);
		String requestedStatus = firstNonBlank(request.rosterStatus(), request.status());
		if (Boolean.TRUE.equals(request.rosterConfirmed()) || "CONFIRMED".equalsIgnoreCase(requestedStatus)) {
			student.confirmRoster();
		} else if ("FLAGGED".equalsIgnoreCase(requestedStatus)) {
			student.flagRoster();
		} else if (Boolean.FALSE.equals(request.rosterConfirmed()) || requestedStatus == null) {
			student.clearRoster();
		} else {
			throw new BadRequestException("Roster status must be CONFIRMED, FLAGGED or PENDING");
		}
		return response(studentRepository.save(student));
	}

	@Transactional
	public StudentResponse update(Long studentId, StudentUpdateRequest request) {
		Student student = requireStudent(studentId);
		SchoolClass schoolClass = classRepository.findById(request.schoolClassId()).orElseThrow(() -> new NotFoundException("School class not found"));
		student.setFullName(request.fullName().trim());
		student.setDateOfBirth(request.dateOfBirth());
		student.setGender(request.gender().trim().toUpperCase());
		student.setSchoolClass(schoolClass);
		return response(studentRepository.save(student));
	}

	@Transactional(readOnly = true)
	public Student requireStudentForUser(User user) {
		return studentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Student profile not found"));
	}

	@Transactional(readOnly = true)
	public List<ParentChildResponse> children(ParentGuardian parent) {
		return linkRepository.findByParentIdOrderByIdAsc(parent.getId()).stream().map(link -> new ParentChildResponse(parent.getId(), parent.getFullName(), link.getStudent().getId(), link.getStudent().getFullName(), link.getStudent().getUser().getEmail(), link.getStudent().getSchoolClass().getName())).toList();
	}

	@Transactional(readOnly = true)
	public Student requireChild(ParentGuardian parent, Long studentId) {
		Student student = requireStudent(studentId);
		if (!linkRepository.existsByParentIdAndStudentId(parent.getId(), studentId)) {
			throw new ForbiddenException("This student is not linked to your account");
		}
		return student;
	}

	@Transactional(readOnly = true)
	public StudentProfileResponse profile(Student student) {
		Term term = catalogService.currentTerm();
		List<TeacherSummaryResponse> teachers = teachersForStudent(student);
		return new StudentProfileResponse(student.getId(), student.getUser().getId(), student.getUser().getEmail(), student.getFullName(), student.getDateOfBirth(), student.getGender(), student.getSchoolClass().getId(), student.getSchoolClass().getName(), student.isRosterConfirmed(), student.getRosterFlag(), student.getAdmissionDate(), term.getId(), term.getName().name(), teachers);
	}

	@Transactional(readOnly = true)
	public List<TeacherSummaryResponse> teachersForStudent(Student student) {
		Map<String, TeacherSummaryResponse> teachers = new LinkedHashMap<>();
		SchoolClass schoolClass = student.getSchoolClass();
		if (schoolClass.getClassTeacher() != null) {
			Teacher classTeacher = schoolClass.getClassTeacher();
			teachers.put(classTeacher.getId() + ":CLASS", new TeacherSummaryResponse(classTeacher.getId(), classTeacher.getFullName(), "Class Teacher"));
		}
		assignmentRepository.findBySchoolClassId(schoolClass.getId()).forEach(assignment -> {
			String key = assignment.getTeacher().getId() + ":" + assignment.getSubject().getId();
			teachers.putIfAbsent(key, new TeacherSummaryResponse(assignment.getTeacher().getId(), assignment.getTeacher().getFullName(), assignment.getSubject().getName()));
		});
		return List.copyOf(teachers.values());
	}

	@Transactional(readOnly = true)
	public StudentResponse response(Student student) {
		return new StudentResponse(student.getId(), student.getUser().getId(), student.getUser().getEmail(), student.getFullName(), student.getDateOfBirth(), student.getGender(), student.getSchoolClass().getId(), student.getSchoolClass().getName(), student.isRosterConfirmed(), student.getRosterFlag(), student.getAdmissionDate());
	}

	@Transactional(readOnly = true)
	public Student requireStudent(Long studentId) {
		return studentRepository.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
	}

	@Transactional(readOnly = true)
	public ParentGuardian requireParentForUser(User user) {
		return parentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Parent profile not found"));
	}

	private SchoolClass resolveOptionalClass(Long schoolClassId, String className) {
		if (schoolClassId != null) {
			return classRepository.findById(schoolClassId).orElseThrow(() -> new NotFoundException("School class not found"));
		}
		if (className != null && !className.isBlank()) {
			return classRepository.findByName(className.trim()).orElseThrow(() -> new NotFoundException("School class not found"));
		}
		return null;
	}

	private void requireClassAccess(Teacher teacher, Student student) {
		if (!teacherService.canAccessClass(teacher, student.getSchoolClass().getId())) {
			throw new ForbiddenException("You are not assigned to this student's class");
		}
	}

	private String firstNonBlank(String first, String second) {
		return first != null && !first.isBlank() ? first : second;
	}
}
