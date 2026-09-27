package com.crestwood.school_management_system.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {
	@Bean
	JavaMailSender javaMailSender(
			@Value("${spring.mail.host}") String host,
			@Value("${spring.mail.port}") int port,
			@Value("${spring.mail.username}") String username,
			@Value("${spring.mail.password}") String password,
			@Value("${spring.mail.properties.mail.smtp.auth}") boolean auth,
			@Value("${spring.mail.properties.mail.smtp.starttls.enable}") boolean starttls,
			@Value("${spring.mail.properties.mail.smtp.connectiontimeout:10000}") int connectionTimeout,
			@Value("${spring.mail.properties.mail.smtp.timeout:10000}") int timeout,
			@Value("${spring.mail.properties.mail.smtp.writetimeout:10000}") int writeTimeout) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(host);
		sender.setPort(port);
		sender.setUsername(username);
		sender.setPassword(password);
		Properties properties = new Properties();
		properties.put("mail.smtp.auth", String.valueOf(auth));
		properties.put("mail.smtp.starttls.enable", String.valueOf(starttls));
		properties.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
		properties.put("mail.smtp.timeout", String.valueOf(timeout));
		properties.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));
		sender.setJavaMailProperties(properties);
		return sender;
	}
}
