package backend.global.config;

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
				.description("Capstone Server API Documentation")
				.version("1.0.0"));
	}
}
