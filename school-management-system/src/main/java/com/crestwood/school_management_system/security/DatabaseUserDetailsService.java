package com.crestwood.school_management_system.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.crestwood.school_management_system.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	public DatabaseUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepository.findByEmailIgnoreCase(username.trim())
				.map(UserPrincipal::new)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
	}
}
