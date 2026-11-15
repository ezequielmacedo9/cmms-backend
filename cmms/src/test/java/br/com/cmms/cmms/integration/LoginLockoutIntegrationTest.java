package br.com.cmms.cmms.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lockout against the real transaction manager. The unit tests mock the
 * repository, so they could not see that the exception reporting a failed
 * attempt also rolled back the counter. This class is intentionally NOT
 * {@code @Transactional}: every request must commit on its own, exactly as
 * in production.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=integration-test-secret-32bytes-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "app.self.url=http://localhost"
})
class LoginLockoutIntegrationTest {

    private static final String EMAIL = "lockout@iso.test";
    private static final String SENHA_CORRETA = "Senha12345";

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(springSecurityFilterChain)
            .build();
    }

    @Test
    @DisplayName("5 senhas erradas bloqueiam a conta: a 6a tentativa e recusada mesmo com a senha certa")
    void login_fiveWrongPasswords_locksAccount() throws Exception {
        registrar();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content(loginJson("SenhaErrada99")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(loginJson(SENHA_CORRETA)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    private void registrar() throws Exception {
        String body = objectMapper.createObjectNode()
            .put("empresaNome", "Empresa Lockout")
            .put("nome", "Admin Lockout")
            .put("email", EMAIL)
            .put("senha", SENHA_CORRETA)
            .toString();
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json").content(body))
            .andExpect(status().isCreated());
    }

    private String loginJson(String senha) {
        return objectMapper.createObjectNode()
            .put("email", EMAIL)
            .put("senha", senha)
            .toString();
    }
}
