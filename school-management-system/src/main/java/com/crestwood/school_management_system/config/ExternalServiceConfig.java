package com.crestwood.school_management_system.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalServiceConfig {
	@Bean
	RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

	@Bean("paystackRestClient")
	RestClient paystackRestClient(@Qualifier("restClientBuilder") RestClient.Builder builder, @Value("${app.paystack.api-url}") String apiUrl) {
		return builder.baseUrl(apiUrl).build();
	}
}
