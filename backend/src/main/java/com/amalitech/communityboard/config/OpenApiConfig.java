package com.amalitech.communityboard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration.
 * UI available at: http://localhost:8080/swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI communityBoardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CommunityBoard API")
                        .description("REST API for the CommunityBoard community announcement platform. " +
                                "Authenticated endpoints require a Bearer JWT token obtained from /api/auth/login.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Team 4 — Amalitech")
                                .email("team4@amalitech.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Provide the JWT token received from /api/auth/login. " +
                                                "Format: Bearer <token>")));
    }
}
