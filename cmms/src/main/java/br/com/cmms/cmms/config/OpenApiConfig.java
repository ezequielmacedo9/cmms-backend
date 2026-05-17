package br.com.cmms.cmms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration. Surfaces an HTTP Bearer JWT scheme,
 * tags for grouping endpoints, and basic metadata. Concrete endpoint
 * documentation lives on the controllers via {@code @Operation} and
 * {@code @ApiResponse} annotations.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.self.url:http://localhost:8080}")
    private String selfUrl;

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearer = new SecurityScheme()
            .name("Authorization")
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT issued by POST /api/auth/login. Prefix the value with 'Bearer '.");

        return new OpenAPI()
            .info(new Info()
                .title("CMMS Industrial Suite API")
                .version("2.0.0")
                .description("""
                    REST API of the CMMS Industrial Suite — equipment, maintenance, parts,
                    tools, dashboards and reports.

                    Errors follow the `ApiError` envelope:
                    `{ timestamp, status, error, code, message, path, traceId, fieldErrors? }`.
                    The `code` field is the stable contract clients should react to.
                    """)
                .contact(new Contact().name("CMMS Team").email("contato@cmms.app"))
                .license(new License().name("Proprietary")))
            .externalDocs(new ExternalDocumentation()
                .description("Project repository")
                .url("https://github.com/ezequielmacedo9/cmms-backend"))
            .servers(List.of(
                new Server().url(selfUrl).description("Current environment"),
                new Server().url("http://localhost:8080").description("Local dev")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components().addSecuritySchemes("bearerAuth", bearer))
            .tags(List.of(
                new Tag().name("Auth").description("Login, refresh token, Google OAuth, password reset, 2FA."),
                new Tag().name("Usuários").description("Gestão de usuários e roles."),
                new Tag().name("Perfil").description("Perfil do usuário autenticado."),
                new Tag().name("Máquinas").description("Cadastro e listagem de equipamentos."),
                new Tag().name("Manutenções").description("Ordens de manutenção preventivas e corretivas."),
                new Tag().name("Peças").description("Estoque de peças."),
                new Tag().name("Ferramentas").description("Cadastro de ferramentas."),
                new Tag().name("Dashboard").description("KPIs e séries históricas do dashboard."),
                new Tag().name("Relatórios").description("Exportações PDF / Excel."),
                new Tag().name("Auditoria").description("Trilha de auditoria das ações sensíveis."),
                new Tag().name("Configurações").description("Configurações do sistema."),
                new Tag().name("Operacional").description("Health check, keep-alive, QR codes.")
            ));
    }
}
