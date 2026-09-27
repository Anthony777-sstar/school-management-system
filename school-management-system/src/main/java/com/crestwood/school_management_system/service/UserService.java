package com.crestwood.school_management_system.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.Accountant;
import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.dto.ApiDtos.UserResponse;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.AccountantRepository;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.TeacherRepository;

@Service
public class UserService {
	private final TeacherRepository teacherRepository;
	private final StudentRepository studentRepository;
	private final ParentGuardianRepository parentRepository;
	private final AccountantRepository accountantRepository;

	public UserService(TeacherRepository teacherRepository, StudentRepository studentRepository, ParentGuardianRepository parentRepository, AccountantRepository accountantRepository) {
		this.teacherRepository = teacherRepository;
		this.studentRepository = studentRepository;
		this.parentRepository = parentRepository;
		this.accountantRepository = accountantRepository;
	}

	@Transactional(readOnly = true)
	public UserResponse response(User user) {
		Long profileId = null;
		String fullName = null;
		switch (user.getRole()) {
			case TEACHER -> {
				Teacher teacher = teacherRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Teacher profile not found"));
				profileId = teacher.getId();
				fullName = teacher.getFullName();
			}
			case STUDENT -> {
				Student student = studentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Student profile not found"));
				profileId = student.getId();
				fullName = student.getFullName();
			}
			case PARENT -> {
				ParentGuardian parent = parentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Parent profile not found"));
				profileId = parent.getId();
				fullName = parent.getFullName();
			}
			case ACCOUNTANT -> {
				Accountant accountant = accountantRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Accountant profile not found"));
				profileId = accountant.getId();
				fullName = accountant.getFullName();
			}
			case SUPER_ADMIN -> fullName = "Anthony Adeyemo";
		}
		return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(), fullName, profileId);
	}

	@Transactional(readOnly = true)
	public Optional<Student> studentForUser(User user) {
		return studentRepository.findByUserId(user.getId());
	}

	@Transactional(readOnly = true)
	public Optional<Teacher> teacherForUser(User user) {
		return teacherRepository.findByUserId(user.getId());
	}

	@Transactional(readOnly = true)
	public Optional<ParentGuardian> parentForUser(User user) {
		return parentRepository.findByUserId(user.getId());
	}

	@Transactional(readOnly = true)
	public Optional<Accountant> accountantForUser(User user) {
		return accountantRepository.findByUserId(user.getId());
	}
}
