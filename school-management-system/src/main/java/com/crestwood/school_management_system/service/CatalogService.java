package com.crestwood.school_management_system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.AcademicSession;
import com.crestwood.school_management_system.domain.entity.SchoolClass;
import com.crestwood.school_management_system.domain.entity.SchoolSettings;
import com.crestwood.school_management_system.domain.entity.Subject;
import com.crestwood.school_management_system.domain.entity.Term;
import com.crestwood.school_management_system.dto.ApiDtos.AcademicContextResponse;
import com.crestwood.school_management_system.dto.ApiDtos.AcademicSessionRequest;
import com.crestwood.school_management_system.dto.ApiDtos.AcademicSessionResponse;
import com.crestwood.school_management_system.dto.ApiDtos.SchoolClassResponse;
import com.crestwood.school_management_system.dto.ApiDtos.SchoolSettingsRequest;
import com.crestwood.school_management_system.dto.ApiDtos.SchoolSettingsResponse;
import com.crestwood.school_management_system.dto.ApiDtos.SubjectResponse;
import com.crestwood.school_management_system.dto.ApiDtos.TermRequest;
import com.crestwood.school_management_system.dto.ApiDtos.TermResponse;
import com.crestwood.school_management_system.exception.BadRequestException;
import com.crestwood.school_management_system.exception.ConflictException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.AcademicSessionRepository;
import com.crestwood.school_management_system.repository.SchoolClassRepository;
import com.crestwood.school_management_system.repository.SchoolSettingsRepository;
import com.crestwood.school_management_system.repository.SubjectRepository;
import com.crestwood.school_management_system.repository.TermRepository;

@Service
public class CatalogService {
	private final SchoolClassRepository classRepository;
	private final SubjectRepository subjectRepository;
	private final AcademicSessionRepository sessionRepository;
	private final TermRepository termRepository;
	private final SchoolSettingsRepository settingsRepository;
	private final String defaultSchoolName;

	public CatalogService(
			SchoolClassRepository classRepository,
			SubjectRepository subjectRepository,
			AcademicSessionRepository sessionRepository,
			TermRepository termRepository,
			SchoolSettingsRepository settingsRepository,
			@Value("${app.school.name}") String defaultSchoolName) {
		this.classRepository = classRepository;
		this.subjectRepository = subjectRepository;
		this.sessionRepository = sessionRepository;
		this.termRepository = termRepository;
		this.settingsRepository = settingsRepository;
		this.defaultSchoolName = defaultSchoolName;
	}

	@Transactional(readOnly = true)
	public List<SchoolClassResponse> classes() {
		return classRepository.findAllByOrderByNameAsc().stream().map(this::classResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<SubjectResponse> subjects() {
		return subjectRepository.findAllByOrderByNameAsc().stream().map(subject -> new SubjectResponse(subject.getId(), subject.getName())).toList();
	}

	@Transactional(readOnly = true)
	public List<AcademicSessionResponse> sessions() {
		return sessionRepository.findAllByOrderByNameDesc().stream().map(this::sessionResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<TermResponse> terms() {
		return termRepository.findAllByOrderByStartDateAsc().stream().map(this::termResponse).toList();
	}

	@Transactional
	public AcademicSessionResponse createSession(AcademicSessionRequest request) {
		String name = request.name().trim();
		if (sessionRepository.findByName(name).isPresent()) {
			throw new ConflictException("Academic session already exists");
		}
		if (request.current()) {
			sessionRepository.findAll().forEach(session -> session.setCurrent(false));
			termRepository.findAll().forEach(term -> term.setCurrent(false));
		}
		return sessionResponse(sessionRepository.save(new AcademicSession(name, request.current())));
	}

	@Transactional
	public AcademicSessionResponse updateSession(Long id, AcademicSessionRequest request) {
		AcademicSession session = sessionRepository.findById(id).orElseThrow(() -> new NotFoundException("Academic session not found"));
		String name = request.name().trim();
		sessionRepository.findByName(name).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
			throw new ConflictException("Academic session already exists");
		});
		if (request.current()) {
			sessionRepository.findAll().stream().filter(existing -> !existing.getId().equals(id)).forEach(existing -> existing.setCurrent(false));
			termRepository.findAll().forEach(term -> term.setCurrent(false));
		}
		session.setName(name);
		session.setCurrent(request.current());
		return sessionResponse(session);
	}

	@Transactional
	public AcademicSessionResponse markSessionCurrent(Long id, boolean current) {
		AcademicSession session = sessionRepository.findById(id).orElseThrow(() -> new NotFoundException("Academic session not found"));
		if (current) {
			sessionRepository.findAll().stream().filter(existing -> !existing.getId().equals(id)).forEach(existing -> existing.setCurrent(false));
			termRepository.findAll().forEach(term -> term.setCurrent(false));
		}
		session.setCurrent(current);
		return sessionResponse(session);
	}

	@Transactional
	public TermResponse createTerm(TermRequest request) {
		Long sessionId = request.academicSessionId() != null ? request.academicSessionId() : sessionRepository.findFirstByCurrentTrue().orElseThrow(() -> new NotFoundException("No current academic session is configured")).getId();
		AcademicSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new NotFoundException("Academic session not found"));
		if (termRepository.findByAcademicSessionIdAndName(session.getId(), request.name()).isPresent()) {
			throw new ConflictException("Term already exists for this academic session");
		}
		validateDates(request.startDate(), request.endDate());
		if (request.current()) {
			termRepository.findAll().forEach(term -> term.setCurrent(false));
		}
		return termResponse(termRepository.save(new Term(session, request.name(), request.current(), request.startDate(), request.endDate())));
	}

	@Transactional
	public TermResponse updateTerm(Long id, TermRequest request) {
		Term term = termRepository.findById(id).orElseThrow(() -> new NotFoundException("Term not found"));
		Long sessionId = request.academicSessionId() != null ? request.academicSessionId() : term.getAcademicSession().getId();
		if (!term.getAcademicSession().getId().equals(sessionId)) {
			throw new BadRequestException("A term cannot be moved to another academic session");
		}
		termRepository.findByAcademicSessionIdAndName(sessionId, request.name()).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
			throw new ConflictException("Term already exists for this academic session");
		});
		validateDates(request.startDate(), request.endDate());
		if (request.current()) {
			termRepository.findAll().stream().filter(existing -> !existing.getId().equals(id)).forEach(existing -> existing.setCurrent(false));
		}
		term.setName(request.name());
		term.setCurrent(request.current());
		term.setStartDate(request.startDate());
		term.setEndDate(request.endDate());
		return termResponse(term);
	}

	@Transactional
	public TermResponse markTermCurrent(Long id, boolean current) {
		Term term = termRepository.findById(id).orElseThrow(() -> new NotFoundException("Term not found"));
		if (current) {
			termRepository.findAll().stream().filter(existing -> !existing.getId().equals(id)).forEach(existing -> existing.setCurrent(false));
		}
		term.setCurrent(current);
		return termResponse(term);
	}

	@Transactional(readOnly = true)
	public Term currentTerm() {
		return termRepository.findFirstByCurrentTrue().orElseThrow(() -> new NotFoundException("No current academic term is configured"));
	}

	@Transactional(readOnly = true)
	public AcademicContextResponse academicContext() {
		Term term = currentTerm();
		AcademicSession session = term.getAcademicSession();
		return new AcademicContextResponse(session.getId(), session.getName(), term.getId(), term.getName().name(), session.getName() + " · " + term.getName().name());
	}

	@Transactional
	public SchoolSettings settings() {
		return settingsRepository.findFirstByOrderByIdAsc().orElseGet(() -> settingsRepository.save(new SchoolSettings(defaultSchoolName, "", "#180D38")));
	}

	@Transactional
	public SchoolSettingsResponse settingsResponse() {
		return settingsResponse(settings());
	}

	@Transactional
	public SchoolSettingsResponse updateSettings(SchoolSettingsRequest request) {
		SchoolSettings settings = settings();
		String color = firstNonBlank(request.primaryColor(), request.primaryBrandColor());
		if (color == null || !color.matches("^#[0-9a-fA-F]{6}$")) {
			throw new BadRequestException("Primary color must be a six-digit hexadecimal color");
		}
		settings.setSchoolName(request.schoolName().trim());
		settings.setPrimaryColor(color.toUpperCase());
		settings.setUpdatedAt(Instant.now());
		return settingsResponse(settingsRepository.save(settings));
	}

	@Transactional
	public SchoolSettingsResponse updateLogo(String logoUrl) {
		SchoolSettings settings = settings();
		settings.setLogoUrl(logoUrl == null ? "" : logoUrl.trim());
		settings.setUpdatedAt(Instant.now());
		return settingsResponse(settingsRepository.save(settings));
	}

	public SchoolClass requireClass(Long id) {
		return classRepository.findById(id).orElseThrow(() -> new NotFoundException("School class not found"));
	}

	public Subject requireSubject(Long id) {
		return subjectRepository.findById(id).orElseThrow(() -> new NotFoundException("Subject not found"));
	}

	public Term requireTerm(Long id) {
		return termRepository.findById(id).orElseThrow(() -> new NotFoundException("Term not found"));
	}

	public SchoolClassResponse classResponse(SchoolClass schoolClass) {
		return new SchoolClassResponse(schoolClass.getId(), schoolClass.getName(), schoolClass.getClassTeacher() == null ? null : schoolClass.getClassTeacher().getId(), schoolClass.getClassTeacher() == null ? null : schoolClass.getClassTeacher().getFullName());
	}

	public TermResponse termResponse(Term term) {
		return new TermResponse(term.getId(), term.getAcademicSession().getId(), term.getAcademicSession().getName(), term.getName(), term.isCurrent(), term.getStartDate(), term.getEndDate());
	}

	public SchoolSettingsResponse settingsResponse(SchoolSettings settings) {
		return new SchoolSettingsResponse(settings.getId(), settings.getSchoolName(), settings.getLogoUrl(), settings.getPrimaryColor(), settings.getPrimaryColor(), settings.getUpdatedAt());
	}

	private AcademicSessionResponse sessionResponse(AcademicSession session) {
		return new AcademicSessionResponse(session.getId(), session.getName(), session.isCurrent());
	}

	private void validateDates(LocalDate startDate, LocalDate endDate) {
		if (endDate.isBefore(startDate)) {
			throw new BadRequestException("Term end date cannot be before its start date");
		}
	}

	private String firstNonBlank(String first, String second) {
		return first != null && !first.isBlank() ? first : second;
	}
}
