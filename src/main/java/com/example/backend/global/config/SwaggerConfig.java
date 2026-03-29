package com.example.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI backendApi() {
		return new OpenAPI()
			.info(new Info()
				.title("Backend API Documentation")
				.description("백엔드 프로젝트 API 명세서")
				.version("1.0.0"));
	}
}
