package br.com.cmms.cmms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {

        SecurityScheme securityScheme = new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        SecurityRequirement securityRequirement =
                new SecurityRequirement().addList("Bearer Authentication");

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "Bearer Authentication",
                                        securityScheme
                                )
                );
    }
}
