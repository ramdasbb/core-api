package com.smartvillage.authservice.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) Configuration for API documentation
 * Configures Bearer token authentication for all protected endpoints
 */
@Configuration
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token for API authentication. Copy the access_token from login response and paste here.\n" +
                  "Format: Bearer <your_access_token>"
)
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Village Orbit - Auth Service API")
                        .version("1.0.0")
                        .description("Authentication and Authorization Service for Village Orbit Platform\n\n" +
                                "**How to use with Swagger UI:**\n" +
                                "1. Call POST /auth/login with your credentials\n" +
                                "2. Copy the 'access_token' from the response\n" +
                                "3. Click the 'Authorize' button (lock icon) at the top-right\n" +
                                "4. Paste the token as: `<your_token>` (without 'Bearer' prefix)\n" +
                                "5. Click 'Authorize' to apply the token to all API calls\n" +
                                "6. All protected endpoints will now include the Authorization header automatically")
                        .contact(new Contact()
                                .name("Smart Village Team")
                                .url("https://smartvillage.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
    }
}
