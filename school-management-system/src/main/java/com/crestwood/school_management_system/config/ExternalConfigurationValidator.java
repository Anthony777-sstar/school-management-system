package com.crestwood.school_management_system.config;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExternalConfigurationValidator implements InitializingBean {
	private final String databaseUrl;
	private final String databaseUsername;
	private final String databasePassword;
	private final String jwtSecret;
	private final long jwtExpirationMs;
	private final String paystackSecretKey;
	private final String paystackPublicKey;
	private final String paystackApiUrl;
	private final String paystackCurrency;
	private final boolean paystackEnabled;
	private final String smtpHost;
	private final int smtpPort;
	private final String smtpUsername;
	private final String smtpPassword;
	private final String mailFrom;
	private final String frontendBaseUrl;
	private final boolean mailEnabled;
	private final String materialsDirectory;
	private final String logoDirectory;
	private final boolean seedEnabled;
	private final String seedPassword;

	public ExternalConfigurationValidator(
			@Value("${spring.datasource.url}") String databaseUrl,
			@Value("${spring.datasource.username}") String databaseUsername,
			@Value("${spring.datasource.password}") String databasePassword,
			@Value("${app.jwt.secret}") String jwtSecret,
			@Value("${app.jwt.expiration-ms}") long jwtExpirationMs,
			@Value("${app.paystack.secret-key}") String paystackSecretKey,
			@Value("${app.paystack.public-key}") String paystackPublicKey,
			@Value("${app.paystack.api-url}") String paystackApiUrl,
			@Value("${app.paystack.currency}") String paystackCurrency,
			@Value("${app.paystack.enabled:false}") boolean paystackEnabled,
			@Value("${spring.mail.host}") String smtpHost,
			@Value("${spring.mail.port}") int smtpPort,
			@Value("${spring.mail.username}") String smtpUsername,
			@Value("${spring.mail.password}") String smtpPassword,
			@Value("${app.mail.from}") String mailFrom,
			@Value("${app.mail.frontend-base-url}") String frontendBaseUrl,
			@Value("${app.mail.enabled:false}") boolean mailEnabled,
			@Value("${app.materials.directory}") String materialsDirectory,
			@Value("${app.uploads.logo-directory}") String logoDirectory,
			@Value("${app.seed.enabled}") boolean seedEnabled,
			@Value("${app.seed.password}") String seedPassword) {
		this.databaseUrl = databaseUrl;
		this.databaseUsername = databaseUsername;
		this.databasePassword = databasePassword;
		this.jwtSecret = jwtSecret;
		this.jwtExpirationMs = jwtExpirationMs;
		this.paystackSecretKey = paystackSecretKey;
		this.paystackPublicKey = paystackPublicKey;
		this.paystackApiUrl = paystackApiUrl;
		this.paystackCurrency = paystackCurrency;
		this.paystackEnabled = paystackEnabled;
		this.smtpHost = smtpHost;
		this.smtpPort = smtpPort;
		this.smtpUsername = smtpUsername;
		this.smtpPassword = smtpPassword;
		this.mailFrom = mailFrom;
		this.frontendBaseUrl = frontendBaseUrl;
		this.mailEnabled = mailEnabled;
		this.materialsDirectory = materialsDirectory;
		this.logoDirectory = logoDirectory;
		this.seedEnabled = seedEnabled;
		this.seedPassword = seedPassword;
	}

	@Override
	public void afterPropertiesSet() {
		require(databaseUrl, "DB_URL");
		require(databaseUsername, "DB_USERNAME");
		require(databasePassword, "DB_PASSWORD");
		if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
		}
		if (jwtExpirationMs <= 0) {
			throw new IllegalStateException("JWT_EXPIRATION_MS must be greater than zero");
		}
		if (paystackEnabled) {
			require(paystackSecretKey, "PAYSTACK_SECRET_KEY");
			require(paystackPublicKey, "PAYSTACK_PUBLIC_KEY");
		}
		require(paystackApiUrl, "PAYSTACK_API_URL");
		require(paystackCurrency, "PAYSTACK_CURRENCY");
		if (mailEnabled) {
			require(smtpHost, "SMTP_HOST");
			require(smtpUsername, "SMTP_USERNAME");
			require(smtpPassword, "SMTP_PASSWORD");
			require(mailFrom, "MAIL_FROM");
			require(frontendBaseUrl, "FRONTEND_BASE_URL");
		}
		if (smtpPort <= 0 || smtpPort > 65535) {
			throw new IllegalStateException("SMTP_PORT must be between 1 and 65535");
		}
		require(materialsDirectory, "MATERIALS_DIRECTORY");
		require(logoDirectory, "LOGO_DIRECTORY");
		if (seedEnabled) {
			String effectiveSeedPassword = seedPassword;
			if (effectiveSeedPassword == null || effectiveSeedPassword.isBlank()) {
				effectiveSeedPassword = System.getenv("SEED_PASSWORD");
			}
			if (effectiveSeedPassword == null || effectiveSeedPassword.length() < 12) {
				throw new IllegalStateException("SEED_PASSWORD must contain at least 12 characters when seeding is enabled");
			}
		}
	}

	private void require(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(name + " is required");
		}
	}
}
