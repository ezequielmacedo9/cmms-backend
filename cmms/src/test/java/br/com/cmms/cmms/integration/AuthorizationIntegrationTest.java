package br.com.cmms.cmms.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role-based authorization matrix at the HTTP boundary. Proves the
 * {@code @PreAuthorize} rules actually block forbidden writes:
 * <ul>
 *   <li>VISUALIZADOR can read machines but cannot create them, nor manage users.</li>
 *   <li>GESTOR can create a machine but cannot delete one (ADMIN+ only).</li>
 *   <li>Anonymous requests never reach a protected endpoint.</li>
 * </ul>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=integration-test-secret-32bytes-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "app.self.url=http://localhost"
})
class AuthorizationIntegrationTest {

    private static final String SENHA = "Senha12345";

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
    @DisplayName("VISUALIZADOR lê mas não escreve; GESTOR cria mas não deleta; anônimo é barrado")
    void authorizationMatrix() throws Exception {
        String admin = registrar("Empresa Authz", "admin-authz@iso.test");
        convidar(admin, "vis-authz@iso.test", "ROLE_VISUALIZADOR");
        convidar(admin, "gestor-authz@iso.test", "ROLE_GESTOR");

        String vis    = login("vis-authz@iso.test");
        String gestor = login("gestor-authz@iso.test");

        // VISUALIZADOR: leitura ok, escrita e gestão de usuários proibidas.
        mockMvc.perform(get("/api/maquinas").header("Authorization", "Bearer " + vis))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/maquinas").header("Authorization", "Bearer " + vis)
                .contentType("application/json").content(maquinaJson("M1")))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + vis))
            .andExpect(status().isForbidden());

        // GESTOR: cria máquina (200) mas não pode deletar (ADMIN+).
        MvcResult criada = mockMvc.perform(post("/api/maquinas").header("Authorization", "Bearer " + gestor)
                .contentType("application/json").content(maquinaJson("M-Gestor")))
            .andExpect(status().isOk())
            .andReturn();
        long maquinaId = parse(criada).path("id").asLong();

        mockMvc.perform(delete("/api/maquinas/" + maquinaId).header("Authorization", "Bearer " + gestor))
            .andExpect(status().isForbidden());

        // Anônimo nunca alcança um endpoint protegido.
        mockMvc.perform(get("/api/dashboard/stats"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private String registrar(String empresa, String email) throws Exception {
        String body = objectMapper.createObjectNode()
            .put("empresaNome", empresa).put("nome", "Admin").put("email", email).put("senha", SENHA)
            .toString();
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json").content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return parse(res).path("accessToken").asText();
    }

    private void convidar(String adminToken, String email, String role) throws Exception {
        String body = objectMapper.createObjectNode()
            .put("nome", "Membro").put("email", email).put("senha", SENHA).put("roleNome", role)
            .toString();
        mockMvc.perform(post("/api/usuarios/convidar")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json").content(body))
            .andExpect(status().isOk());
    }

    private String login(String email) throws Exception {
        String body = objectMapper.createObjectNode().put("email", email).put("senha", SENHA).toString();
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json").content(body))
            .andExpect(status().isOk())
            .andReturn();
        return parse(res).path("accessToken").asText();
    }

    private String maquinaJson(String nome) {
        return objectMapper.createObjectNode()
            .put("nome", nome).put("setor", "Setor").put("status", "ATIVO")
            .toString();
    }

    private JsonNode parse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
