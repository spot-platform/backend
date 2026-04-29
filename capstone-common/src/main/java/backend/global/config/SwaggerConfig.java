package backend.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI backendApi() {
		SecurityScheme bearerScheme = new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT")
			.in(SecurityScheme.In.HEADER)
			.name("Authorization");

		return new OpenAPI()
			.info(new Info()
				.title("Backend API Documentation")
				.description("Capstone Server API Documentation")
				.version("1.0.0"))
			.components(new Components().addSecuritySchemes("BearerAuth", bearerScheme))
			.security(List.of(new SecurityRequirement().addList("BearerAuth")));
	}
}
