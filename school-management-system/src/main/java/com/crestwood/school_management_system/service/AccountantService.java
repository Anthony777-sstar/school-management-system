package com.crestwood.school_management_system.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crestwood.school_management_system.domain.entity.Accountant;
import com.crestwood.school_management_system.domain.entity.User;
import com.crestwood.school_management_system.domain.enums.Role;
import com.crestwood.school_management_system.dto.ApiDtos.AccountantCreateRequest;
import com.crestwood.school_management_system.dto.ApiDtos.AccountantResponse;
import com.crestwood.school_management_system.dto.ApiDtos.AccountantUpdateRequest;
import com.crestwood.school_management_system.exception.ConflictException;
import com.crestwood.school_management_system.exception.NotFoundException;
import com.crestwood.school_management_system.repository.AccountantRepository;
import com.crestwood.school_management_system.repository.UserRepository;

@Service
public class AccountantService {
	private final UserRepository userRepository;
	private final AccountantRepository accountantRepository;
	private final PasswordEncoder passwordEncoder;
	private final CredentialService credentialService;

	public AccountantService(UserRepository userRepository, AccountantRepository accountantRepository, PasswordEncoder passwordEncoder, CredentialService credentialService) {
		this.userRepository = userRepository;
		this.accountantRepository = accountantRepository;
		this.passwordEncoder = passwordEncoder;
		this.credentialService = credentialService;
	}

	@Transactional(readOnly = true)
	public List<AccountantResponse> list() {
		return accountantRepository.findAllByOrderByFullNameAsc().stream().map(this::response).toList();
	}

	@Transactional
	public AccountantResponse create(AccountantCreateRequest request) {
		String email = request.email().trim().toLowerCase();
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("A user with this email already exists");
		}
		String password = request.password() == null || request.password().isBlank() ? credentialService.createTemporaryPassword() : request.password();
		User user = userRepository.save(new User(email, passwordEncoder.encode(password), Role.ACCOUNTANT));
		Accountant accountant = accountantRepository.save(new Accountant(user, request.fullName().trim(), request.phone().trim()));
		return response(accountant, password);
	}

	@Transactional
	public AccountantResponse update(Long id, AccountantUpdateRequest request) {
		Accountant accountant = requireAccountant(id);
		accountant.setFullName(request.fullName().trim());
		accountant.setPhone(request.phone().trim());
		return response(accountantRepository.save(accountant));
	}

	@Transactional
	public void disable(Long id) {
		Accountant accountant = requireAccountant(id);
		accountant.getUser().setEnabled(false);
		userRepository.save(accountant.getUser());
	}

	@Transactional(readOnly = true)
	public Accountant requireAccountantForUser(User user) {
		return accountantRepository.findByUserId(user.getId()).orElseThrow(() -> new NotFoundException("Accountant profile not found"));
	}

	@Transactional(readOnly = true)
	public Accountant requireAccountant(Long id) {
		return accountantRepository.findById(id).orElseThrow(() -> new NotFoundException("Accountant not found"));
	}

	@Transactional(readOnly = true)
	public AccountantResponse response(Accountant accountant) {
		return response(accountant, null);
	}

	@Transactional(readOnly = true)
	public AccountantResponse response(Accountant accountant, String temporaryPassword) {
		return new AccountantResponse(accountant.getId(), accountant.getUser().getId(), accountant.getUser().getEmail(), accountant.getFullName(), accountant.getPhone(), accountant.getUser().isEnabled(), temporaryPassword);
	}
}
