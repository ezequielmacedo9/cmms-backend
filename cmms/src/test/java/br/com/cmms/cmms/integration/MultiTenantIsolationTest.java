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
 * End-to-end proof of tenant isolation. Registers two independent empresas,
 * each with its own admin + machine, and verifies that empresa A cannot read,
 * list, or delete empresa B's data. A cross-tenant access returns 404 (not
 * 403) so the API never leaks the existence of another tenant's resource.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=integration-test-secret-32bytes-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "app.self.url=http://localhost"
})
class MultiTenantIsolationTest {

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
    @DisplayName("Empresa A não enxerga, lê ou exclui dados da empresa B")
    void tenantIsolation() throws Exception {
        String tokenA = registrar("Empresa A", "admin-a@iso.test");
        String tokenB = registrar("Empresa B", "admin-b@iso.test");

        long maquinaA = criarMaquina(tokenA, "Maquina A", "Setor A");
        long maquinaB = criarMaquina(tokenB, "Maquina B", "Setor B");

        // A lista apenas a própria máquina.
        MvcResult listA = mockMvc.perform(get("/api/maquinas").param("unpaged", "true")
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode arr = parse(listA);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).path("nome").asText()).isEqualTo("Maquina A");

        // A não consegue LER a máquina de B (404, não 403).
        mockMvc.perform(get("/api/maquinas/" + maquinaB)
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isNotFound());

        // A não consegue EXCLUIR a máquina de B.
        mockMvc.perform(delete("/api/maquinas/" + maquinaB)
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isNotFound());

        // Sanidade: A lê a própria máquina; B continua enxergando a sua.
        mockMvc.perform(get("/api/maquinas/" + maquinaA)
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/maquinas/" + maquinaB)
                .header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isOk());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private String registrar(String empresa, String email) throws Exception {
        String body = objectMapper.createObjectNode()
            .put("empresaNome", empresa)
            .put("nome", "Admin")
            .put("email", email)
            .put("senha", "Senha12345")
            .toString();
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json").content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return parse(res).path("accessToken").asText();
    }

    private long criarMaquina(String token, String nome, String setor) throws Exception {
        String body = objectMapper.createObjectNode()
            .put("nome", nome)
            .put("setor", setor)
            .put("status", "ATIVO")
            .toString();
        MvcResult res = mockMvc.perform(post("/api/maquinas")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json").content(body))
            .andExpect(status().isOk())
            .andReturn();
        return parse(res).path("id").asLong();
    }

    private JsonNode parse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
