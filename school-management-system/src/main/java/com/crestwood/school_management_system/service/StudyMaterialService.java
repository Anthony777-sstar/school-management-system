package com.crestwood.school_management_system.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.crestwood.school_management_system.domain.entity.ParentGuardian;
import com.crestwood.school_management_system.domain.entity.SchoolClass;
import com.crestwood.school_management_system.domain.entity.Student;
import com.crestwood.school_management_system.domain.entity.StudyMaterial;
import com.crestwood.school_management_system.domain.entity.Subject;
import com.crestwood.school_management_system.domain.entity.Teacher;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.dto.ApiDtos.StudyMaterialResponse;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ForbiddenException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.exception.ServiceUnavailableException;
import com.crestwood.school_management_system.repository.ParentGuardianRepository;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.StudentParentLinkRepository;
import com.crestwood.school_management_system.repository.StudentRepository;
import com.crestwood.school_management_system.repository.StudyMaterialRepository;
import com.crestwood.school_management_system.repository.SubjectRepository;
import com.crestwood.school_management_system.repository.TeacherRepository;

@Service
public class StudyMaterialService {
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "png", "jpg", "jpeg", "gif", "webp", "zip");
	private final StudyMaterialRepository materialRepository;
	private final SubjectRepository subjectRepository;
	private final SchoolClassRepository classRepository;
	private final StudentRepository studentRepository;
	private final TeacherRepository teacherRepository;
	private final ParentGuardianRepository parentRepository;
	private final StudentParentLinkRepository linkRepository;
	private final TeacherService teacherService;
	private final Path root;
	private final long maxFileSizeBytes;

	public StudyMaterialService(
			StudyMaterialRepository materialRepository,
			SubjectRepository subjectRepository,
			SchoolClassRepository classRepository,
			StudentRepository studentRepository,
			TeacherRepository teacherRepository,
			ParentGuardianRepository parentRepository,
			StudentParentLinkRepository linkRepository,
			TeacherService teacherService,
			@Value("${app.materials.directory}") String directory,
			@Value("${app.materials.max-file-size-bytes}") long maxFileSizeBytes) {
		this.materialRepository = materialRepository;
		this.subjectRepository = subjectRepository;
		this.classRepository = classRepository;
		this.studentRepository = studentRepository;
		this.teacherRepository = teacherRepository;
		this.parentRepository = parentRepository;
		this.linkRepository = linkRepository;
		this.teacherService = teacherService;
		this.root = Path.of(directory).toAbsolutePath().normalize();
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	@Transactional
	public StudyMaterialResponse upload(Teacher teacher, Long subjectId, Long schoolClassId, String title, MultipartFile file) {
		validateUpload(subjectId, schoolClassId, title, file);
		Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new NotFoundException("Subject not found"));
		SchoolClass schoolClass = classRepository.findById(schoolClassId).orElseThrow(() -> new NotFoundException("School class not found"));
		if (!teacherService.canTeach(teacher, subjectId, schoolClassId)) {
			throw new ForbiddenException("You are not assigned to teach this subject and class");
		}
		String originalFilename = safeOriginalFilename(file.getOriginalFilename());
		String extension = extension(originalFilename);
		String storedFilename = UUID.randomUUID() + extension;
		Path destination = root.resolve(storedFilename).normalize();
		if (!destination.startsWith(root)) {
			throw new BadRequestException("Invalid study material filename");
		}
		try {
			Files.createDirectories(root);
			Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
			StudyMaterial material = materialRepository.save(new StudyMaterial(teacher, subject, schoolClass, title.trim(), storedFilename, originalFilename, normalizedContentType(file.getContentType()), file.getSize()));
			return response(material);
		} catch (IOException exception) {
			throw new ServiceUnavailableException("Study material storage is unavailable");
		} catch (RuntimeException exception) {
			deleteQuietly(destination);
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public List<StudyMaterialResponse> listForUser(User user, Long schoolClassId, Long subjectId) {
		return switch (user.getRole()) {
			case SUPER_ADMIN -> filtered(materialRepository.findAllByOrderByUploadedAtDesc(), schoolClassId, subjectId);
			case TEACHER -> {
				Teacher teacher = teacherService.requireTeacherForUser(user);
				if (schoolClassId != null && !teacherService.canAccessClass(teacher, schoolClassId)) {
					throw new ForbiddenException("You are not assigned to this class");
				}
				List<StudyMaterial> allowed = materialRepository.findAllByOrderByUploadedAtDesc().stream().filter(material -> material.getTeacher().getId().equals(teacher.getId()) || teacherService.canTeach(teacher, material.getSubject().getId(), material.getSchoolClass().getId())).toList();
				yield filtered(allowed, schoolClassId, subjectId);
			}
			case STUDENT -> {
				Student student = studentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Student profile not found"));
				yield filtered(materialRepository.findBySchoolClassIdOrderByUploadedAtDesc(student.getSchoolClass().getId()), null, subjectId);
			}
			case PARENT -> {
				ParentGuardian parent = parentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Parent profile not found"));
				Set<Long> childClassIds = new LinkedHashSet<>();
				linkRepository.findByParentIdOrderByIdAsc(parent.getId()).forEach(link -> childClassIds.add(link.getStudent().getSchoolClass().getId()));
				List<StudyMaterial> allowed = materialRepository.findAllByOrderByUploadedAtDesc().stream().filter(material -> childClassIds.contains(material.getSchoolClass().getId())).toList();
				yield filtered(allowed, schoolClassId, subjectId);
			}
			case ACCOUNTANT -> throw new ForbiddenException("Accountants cannot access study materials");
		};
	}

	@Transactional(readOnly = true)
	public StudyMaterial require(Long id) {
		return materialRepository.findById(id).orElseThrow(() -> new NotFoundException("Study material not found"));
	}

	@Transactional(readOnly = true)
	public Resource resource(StudyMaterial material) {
		Path path = root.resolve(material.getStoredFilename()).normalize();
		if (!path.startsWith(root) || !Files.isRegularFile(path)) {
			throw new NotFoundException("Study material file not found");
		}
		return new FileSystemResource(path);
	}

	@Transactional(readOnly = true)
	public void requireDownloadAccess(StudyMaterial material, User user) {
		switch (user.getRole()) {
			case SUPER_ADMIN -> {
			}
			case TEACHER -> {
				Teacher teacher = teacherRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Teacher profile not found"));
				if (!teacher.getId().equals(material.getTeacher().getId()) && !teacherService.canTeach(teacher, material.getSubject().getId(), material.getSchoolClass().getId())) {
					throw new ForbiddenException("You are not assigned to this study material");
				}
			}
			case STUDENT -> {
				Student student = studentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Student profile not found"));
				if (!student.getSchoolClass().getId().equals(material.getSchoolClass().getId())) {
					throw new ForbiddenException("This study material is not assigned to your class");
				}
			}
			case PARENT -> {
				ParentGuardian parent = parentRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Parent profile not found"));
				boolean allowed = linkRepository.findByParentIdOrderByIdAsc(parent.getId()).stream().anyMatch(link -> link.getStudent().getSchoolClass().getId().equals(material.getSchoolClass().getId()));
				if (!allowed) {
					throw new ForbiddenException("This study material is not assigned to your child's class");
				}
			}
			case ACCOUNTANT -> throw new ForbiddenException("Accountants cannot access study materials");
		}
	}

	@Transactional
	public void delete(User user, Long id) {
		StudyMaterial material = require(id);
		if (user.getRole() == Role.TEACHER && !material.getTeacher().getUser().getId().equals(user.getId())) {
			throw new ForbiddenException("You can only delete your own study materials");
		}
		if (user.getRole() != Role.SUPER_ADMIN && user.getRole() != Role.TEACHER) {
			throw new ForbiddenException("You cannot delete study materials");
		}
		materialRepository.delete(material);
		deleteQuietly(root.resolve(material.getStoredFilename()).normalize());
	}

	@Transactional(readOnly = true)
	public StudyMaterialResponse response(StudyMaterial material) {
		return new StudyMaterialResponse(material.getId(), material.getTeacher().getId(), material.getTeacher().getFullName(), material.getSubject().getId(), material.getSubject().getName(), material.getSchoolClass().getId(), material.getSchoolClass().getName(), material.getTitle(), "/api/v1/study-materials/" + material.getId() + "/download", material.getOriginalFilename(), material.getContentType(), material.getFileSize(), material.getUploadedAt());
	}

	private void validateUpload(Long subjectId, Long schoolClassId, String title, MultipartFile file) {
		if (subjectId == null || schoolClassId == null) {
			throw new BadRequestException("Class and subject are required");
		}
		if (title == null || title.isBlank() || title.length() > 200) {
			throw new BadRequestException("Study material title is required and must not exceed 200 characters");
		}
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("A study material file is required");
		}
		if (file.getSize() > maxFileSizeBytes) {
			throw new BadRequestException("Study material file exceeds the configured size limit");
		}
		String filename = safeOriginalFilename(file.getOriginalFilename());
		if (!ALLOWED_EXTENSIONS.contains(extension(filename))) {
			throw new BadRequestException("Study material file type is not allowed");
		}
	}

	private List<StudyMaterialResponse> filtered(List<StudyMaterial> materials, Long schoolClassId, Long subjectId) {
		return materials.stream().filter(material -> schoolClassId == null || material.getSchoolClass().getId().equals(schoolClassId)).filter(material -> subjectId == null || material.getSubject().getId().equals(subjectId)).map(this::response).toList();
	}

	private String safeOriginalFilename(String filename) {
		if (filename == null || filename.isBlank()) {
			return "material";
		}
		String normalized = filename.replace('\\', '/').replaceAll("[\\p{Cntrl}]", "_");
		String safe = Path.of(normalized).getFileName().toString();
		return safe.length() > 255 ? safe.substring(safe.length() - 255) : safe;
	}

	private String extension(String filename) {
		int dot = filename.lastIndexOf('.');
		return dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
	}

	private String normalizedContentType(String contentType) {
		return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
	}

	private void deleteQuietly(Path path) {
		if (!path.startsWith(root)) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
		}
	}

}
