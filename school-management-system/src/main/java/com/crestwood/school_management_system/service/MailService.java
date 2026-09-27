package com.crestwood.school_management_system.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.crestwood.school_management_system.exception.ServiceUnavailableException;

@Service
public class MailService {
	private final JavaMailSender mailSender;
	private final String host;
	private final String username;
	private final String password;
	private final String from;
	private final String frontendBaseUrl;
	private final boolean mailEnabled;

	public MailService(
			JavaMailSender mailSender,
			@Value("${spring.mail.host}") String host,
			@Value("${spring.mail.username}") String username,
			@Value("${spring.mail.password}") String password,
			@Value("${app.mail.from}") String from,
			@Value("${app.mail.frontend-base-url}") String frontendBaseUrl,
			@Value("${app.mail.enabled:false}") boolean mailEnabled) {
		this.mailSender = mailSender;
		this.host = host;
		this.username = username;
		this.password = password;
		this.from = from;
		this.frontendBaseUrl = frontendBaseUrl;
		this.mailEnabled = mailEnabled;
	}

	public void sendEnrollmentApproval(String to, String studentName, String parentName, String studentEmail, String parentEmail, String temporaryPassword) {
		String body = "Dear " + parentName + ",\n\nCrestwood Academy has approved the enrollment application for " + studentName + ".\n\nStudent login: " + studentEmail + "\nParent login: " + parentEmail + "\nTemporary password: " + temporaryPassword + "\n\nPlease sign in and change the temporary password immediately.";
		send(to, "Crestwood Academy enrollment approved", body);
	}

	public void sendPasswordReset(String to, String token) {
		String resetUrl = frontendBaseUrl.replaceAll("/$", "") + "/#/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
		send(to, "Reset your Crestwood Academy password", "Use this link to reset your password:\n\n" + resetUrl + "\n\nThe link expires in 30 minutes.");
	}

	public void sendResultPosted(String to, String studentName, String subjectName, String termName, int total, String grade) {
		send(to, "New result posted", "Dear " + studentName + ",\n\nYour " + subjectName + " result for " + termName + " has been posted.\nTotal: " + total + "\nGrade: " + grade);
	}

	public void sendFeeDue(String to, String studentName, String className, String termName, String amount, String dueDate) {
		send(to, "School fee due", "Dear " + studentName + ",\n\nThe " + termName + " fee for " + className + " is N" + amount + " and is due on " + dueDate + ".");
	}

	public void sendFeeReminder(String to, String studentName, String className, String termName, String amount, String dueDate) {
		send(to, "School fee reminder", "This is a reminder that the " + termName + " fee for " + className + " is N" + amount + " and was due on " + dueDate + ".");
	}

	private static final Logger logger = LoggerFactory.getLogger(MailService.class);

	public boolean isEnabled() {
		return mailEnabled && host != null && !host.isBlank() && username != null && !username.isBlank() && password != null && !password.isBlank() && from != null && !from.isBlank();
	}

	public boolean deliver(Runnable action) {
		if (!isEnabled()) {
			return false;
		}
		try {
			action.run();
			return true;
		} catch (ServiceUnavailableException exception) {
			logger.warn("Email delivery failed: {}", exception.getMessage());
			return false;
		}
	}

	private void send(String to, String subject, String body) {
		requireConfigured();
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(body);
		try {
			mailSender.send(message);
		} catch (MailException exception) {
			throw new ServiceUnavailableException("Email service is unavailable");
		}
	}

	private void requireConfigured() {
		if (!mailEnabled) {
			throw new ServiceUnavailableException("Email delivery is disabled. Set MAIL_ENABLED=true with SMTP credentials to send email.");
		}
		if (host == null || host.isBlank() || username == null || username.isBlank() || password == null || password.isBlank() || from == null || from.isBlank() || frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
			throw new ServiceUnavailableException("Email service is unavailable: SMTP and mail URL configuration are required");
		}
	}
}
