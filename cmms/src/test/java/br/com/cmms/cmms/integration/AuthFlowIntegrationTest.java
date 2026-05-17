package br.com.cmms.cmms.integration;

import br.com.cmms.cmms.dto.LoginRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.web.FilterChainProxy;
import br.com.cmms.cmms.Security.TraceIdFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test that wires the full Spring context against the
 * in-memory H2 database. Covers the most critical user journey:
 *
 * <ol>
 *   <li>Public endpoint /ping is reachable without authentication.</li>
 *   <li>Login with the seed dev user issues a JWT.</li>
 *   <li>Token grants access to a protected endpoint (/api/dashboard/stats).</li>
 *   <li>Bad credentials are rejected with the canonical ApiError envelope.</li>
 *   <li>X-Trace-Id is echoed by the TraceIdFilter.</li>
 * </ol>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=integration-test-secret-32bytes-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "app.self.url=http://localhost",
    "app.bootstrap.dev-password=dev123!"
})
class AuthFlowIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private TraceIdFilter traceIdFilter;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    void buildMockMvc() {
        if (mockMvc == null) {
            // TraceIdFilter must run first so its MDC value is available to
            // the security chain and downstream handlers.
            mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(traceIdFilter, springSecurityFilterChain)
                .build();
        }
    }

    @Test
    @DisplayName("/ping é público (não exige Authorization)")
    void pingIsPublic() throws Exception {
        buildMockMvc();
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login com seed dev user devolve accessToken e role")
    void loginIssuesToken() throws Exception {
        buildMockMvc();
        String body = loginJson("superadmin@email.com", "dev123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.role").value("ROLE_SUPER_ADMIN"))
            .andReturn();

        String token = parse(result).path("accessToken").asText();
        assertThat(token.split("\\.")).hasSize(3); // JWT shape
    }

    @Test
    @DisplayName("Endpoint protegido aceita Bearer token emitido no login")
    void protectedEndpointAcceptsToken() throws Exception {
        buildMockMvc();
        String body = loginJson("superadmin@email.com", "dev123!");
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andReturn();
        String token = parse(login).path("accessToken").asText();

        mockMvc.perform(get("/api/dashboard/stats")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalMaquinas").exists())
            .andExpect(jsonPath("$.totalManutencoes").exists());
    }

    @Test
    @DisplayName("Endpoint protegido sem Bearer é rejeitado (401 ou 403)")
    void protectedEndpointRejectsAnonymous() throws Exception {
        // Spring Security devolve 403 (Forbidden) por padrão para anonymous;
        // o JwtAuthFilter só responde 401 quando o Bearer está presente e
        // inválido. Aceitamos qualquer um dos dois — a essência do teste é
        // garantir que a request anônima NÃO entra no endpoint.
        buildMockMvc();
        mockMvc.perform(get("/api/dashboard/stats"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assertThat(status).isIn(401, 403);
            });
    }

    @Test
    @DisplayName("Bad credentials retornam 401 com code BAD_CREDENTIALS")
    void badCredentialsReturnUnauthorized() throws Exception {
        buildMockMvc();
        String body = loginJson("superadmin@email.com", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("TraceIdFilter ecoa X-Trace-Id no response")
    void traceIdHeaderIsEchoed() throws Exception {
        buildMockMvc();
        mockMvc.perform(get("/ping").header("X-Trace-Id", "test-trace-001"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String echoed = result.getResponse().getHeader("X-Trace-Id");
                assertThat(echoed).isEqualTo("test-trace-001");
            });
    }

    @Test
    @DisplayName("Sem X-Trace-Id de entrada, o filtro gera um novo")
    void traceIdGeneratedWhenMissing() throws Exception {
        buildMockMvc();
        mockMvc.perform(get("/ping"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String generated = result.getResponse().getHeader("X-Trace-Id");
                assertThat(generated).isNotBlank();
                assertThat(generated).hasSizeBetween(8, 64);
            });
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private String loginJson(String email, String senha) throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(email);
        dto.setSenha(senha);
        return objectMapper.writeValueAsString(dto);
    }

    private JsonNode parse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
