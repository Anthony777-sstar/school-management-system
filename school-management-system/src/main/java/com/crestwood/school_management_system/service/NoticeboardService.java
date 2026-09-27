package com.crestwood.school_management_system.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.NoticeboardPost;
import com.crestwood.school_management_system.domain.entity.SchoolClass;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.TeacherSubjectClass;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.NoticeboardAudience;
import com.crestwood.school_management_system.domain.enums.NotificationType;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.dto.ApiDtos.NoticeboardCreateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.NoticeboardResponse;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.AccountantRepository;
import com.crestwood.school_management_system.repository.NoticeboardPostRepository;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.TeacherRepository;
import com.crestwood.school_management_system.repository.TeacherSubjectClassRepository;
import com.crestwood.school_management_system.repository.UserRepository;

@Service
public class NoticeboardService {
	private final NoticeboardPostRepository postRepository;
	private final SchoolClassRepository classRepository;
	private final StudentRepository studentRepository;
	private final TeacherRepository teacherRepository;
	private final ParentGuardianRepository parentRepository;
	private final AccountantRepository accountantRepository;
	private final UserRepository userRepository;
	private final StudentParentLinkRepository linkRepository;
	private final TeacherSubjectClassRepository assignmentRepository;
	private final NotificationService notificationService;
	private final UserService userService;
	private final TeacherService teacherService;

	public NoticeboardService(
			NoticeboardPostRepository postRepository,
			SchoolClassRepository classRepository,
			StudentRepository studentRepository,
			TeacherRepository teacherRepository,
			ParentGuardianRepository parentRepository,
			AccountantRepository accountantRepository,
			UserRepository userRepository,
			StudentParentLinkRepository linkRepository,
			TeacherSubjectClassRepository assignmentRepository,
			NotificationService notificationService,
			UserService userService,
			TeacherService teacherService) {
		this.postRepository = postRepository;
		this.classRepository = classRepository;
		this.studentRepository = studentRepository;
		this.teacherRepository = teacherRepository;
		this.parentRepository = parentRepository;
		this.accountantRepository = accountantRepository;
		this.userRepository = userRepository;
		this.linkRepository = linkRepository;
		this.assignmentRepository = assignmentRepository;
		this.notificationService = notificationService;
		this.userService = userService;
		this.teacherService = teacherService;
	}

	@Transactional
	public NoticeboardResponse create(User user, NoticeboardCreateRequest request) {
		if (user.getRole() != Role.SUPER_ADMIN && user.getRole() != Role.TEACHER) {
			throw new ForbiddenException("Only administrators and teachers can post notices");
		}
		SchoolClass audienceClass = null;
		if (request.audience() == NoticeboardAudience.SPECIFIC_CLASS) {
			if (request.audienceClassId() == null) {
				throw new BadRequestException("An audience class is required");
			}
			audienceClass = classRepository.findById(request.audienceClassId()).orElseThrow(() -> new NotFoundException("School class not found"));
			if (user.getRole() == Role.TEACHER && !teacherService.canAccessClass(teacherService.requireTeacherForUser(user), audienceClass.getId())) {
				throw new ForbiddenException("You are not assigned to the audience class");
			}
		} else if (request.audienceClassId() != null) {
			throw new BadRequestException("Audience class is only valid for SPECIFIC_CLASS posts");
		}
		NoticeboardPost post = postRepository.save(new NoticeboardPost(request.title().trim(), request.body().trim(), user, request.audience(), audienceClass));
		notifyAudience(post);
		return response(post);
	}

	@Transactional(readOnly = true)
	public List<NoticeboardResponse> list(User user) {
		return postRepository.findAllByOrderByPostedAtDesc().stream().filter(post -> visible(post, user)).map(this::response).toList();
	}

	@Transactional
	public void delete(User user, Long id) {
		NoticeboardPost post = postRepository.findById(id).orElseThrow(() -> new NotFoundException("Noticeboard post not found"));
		if (user.getRole() != Role.SUPER_ADMIN && !post.getPostedByUser().getId().equals(user.getId())) {
			throw new ForbiddenException("You can only delete your own posts");
		}
		postRepository.delete(post);
	}

	@Transactional(readOnly = true)
	public NoticeboardResponse response(NoticeboardPost post) {
		String name = userService.response(post.getPostedByUser()).fullName();
		return new NoticeboardResponse(post.getId(), post.getTitle(), post.getBody(), post.getPostedByUser().getId(), name, post.getAudience(), post.getAudienceClass() == null ? null : post.getAudienceClass().getId(), post.getAudienceClass() == null ? null : post.getAudienceClass().getName(), post.getPostedAt());
	}

	private boolean visible(NoticeboardPost post, User user) {
		if (user.getRole() == Role.SUPER_ADMIN) {
			return true;
		}
		return switch (post.getAudience()) {
			case ALL -> true;
			case STUDENTS -> user.getRole() == Role.STUDENT;
			case PARENTS -> user.getRole() == Role.PARENT;
			case TEACHERS -> user.getRole() == Role.TEACHER;
			case SPECIFIC_CLASS -> classVisible(post.getAudienceClass(), user);
		};
	}

	private boolean classVisible(SchoolClass schoolClass, User user) {
		if (schoolClass == null) {
			return false;
		}
		return switch (user.getRole()) {
			case STUDENT -> studentRepository.findByUserId(user.getId()).map(student -> student.getSchoolClass().getId().equals(schoolClass.getId())).orElse(false);
			case PARENT -> parentRepository.findByUserId(user.getId()).map(parent -> linkRepository.findByParentIdOrderByIdAsc(parent.getId()).stream().anyMatch(link -> link.getStudent().getSchoolClass().getId().equals(schoolClass.getId()))).orElse(false);
			case TEACHER -> teacherRepository.findByUserId(user.getId()).map(teacher -> teacherService.canAccessClass(teacher, schoolClass.getId())).orElse(false);
			case SUPER_ADMIN, ACCOUNTANT -> user.getRole() == Role.SUPER_ADMIN;
		};
	}

	private void notifyAudience(NoticeboardPost post) {
		Set<User> recipients = new LinkedHashSet<>();
		switch (post.getAudience()) {
			case ALL -> userRepository.findAll().forEach(user -> recipients.add(user));
			case STUDENTS -> studentRepository.findAll().forEach(student -> recipients.add(student.getUser()));
			case PARENTS -> parentRepository.findAll().forEach(parent -> recipients.add(parent.getUser()));
			case TEACHERS -> teacherRepository.findAll().forEach(teacher -> recipients.add(teacher.getUser()));
			case SPECIFIC_CLASS -> collectSpecificClassRecipients(post.getAudienceClass(), recipients);
		}
		notificationService.createAll(recipients, NotificationType.NOTICEBOARD, post.getTitle(), post.getBody());
	}

	private void collectSpecificClassRecipients(SchoolClass schoolClass, Set<User> recipients) {
		if (schoolClass == null) {
			return;
		}
		List<Student> students = studentRepository.findBySchoolClassIdOrderByFullNameAsc(schoolClass.getId());
		students.forEach(student -> recipients.add(student.getUser()));
		Set<Long> parentIds = new LinkedHashSet<>();
		for (Student student : students) {
			linkRepository.findParentsByStudentId(student.getId()).forEach(parent -> parentIds.add(parent.getId()));
		}
		for (Long parentId : parentIds) {
			parentRepository.findById(parentId).ifPresent(parent -> recipients.add(parent.getUser()));
		}
		Set<Long> teacherIds = new LinkedHashSet<>();
		for (TeacherSubjectClass assignment : assignmentRepository.findBySchoolClassId(schoolClass.getId())) {
			teacherIds.add(assignment.getTeacher().getId());
		}
		if (schoolClass.getClassTeacher() != null) {
			teacherIds.add(schoolClass.getClassTeacher().getId());
		}
		for (Long teacherId : teacherIds) {
			teacherRepository.findById(teacherId).ifPresent(teacher -> recipients.add(teacher.getUser()));
		}
	}
}
