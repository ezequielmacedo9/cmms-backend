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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Billing flow end-to-end: a fresh empresa starts on a TRIAL/STARTER
 * subscription, the plan catalog lists the four tiers, and checkout activates
 * the chosen plan (no payment gateway yet → empty payment link).
 */
@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=integration-test-secret-32bytes-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "app.self.url=http://localhost"
})
class BillingFlowIntegrationTest {

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
    @DisplayName("Trial inicial, catálogo de planos e checkout ativando o plano")
    void billingFlow() throws Exception {
        String token = registrar("Empresa Billing", "admin-billing@iso.test");

        // Nova empresa comeca em TRIAL/STARTER com acesso.
        MvcResult minha = mockMvc.perform(get("/api/billing/minha")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode a = parse(minha);
        assertThat(a.path("status").asText()).isEqualTo("TRIAL");
        assertThat(a.path("plano").asText()).isEqualTo("STARTER");
        assertThat(a.path("acesso").asBoolean()).isTrue();
        assertThat(a.path("trialAtivo").asBoolean()).isTrue();

        // Catalogo expoe os 4 planos.
        MvcResult planos = mockMvc.perform(get("/api/billing/planos")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode p = parse(planos);
        assertThat(p.has("STARTER") && p.has("PRO") && p.has("BUSINESS") && p.has("ENTERPRISE")).isTrue();

        // Checkout ativa o plano PRO; link de pagamento vazio (sem gateway).
        MvcResult checkout = mockMvc.perform(post("/api/billing/checkout")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"plano\":\"PRO\"}"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode c = parse(checkout);
        assertThat(c.path("assinatura").path("plano").asText()).isEqualTo("PRO");
        assertThat(c.path("assinatura").path("status").asText()).isEqualTo("ATIVA");
        assertThat(c.path("linkPagamento").asText()).isEmpty();

        // E a assinatura persistiu como PRO/ATIVA.
        MvcResult minha2 = mockMvc.perform(get("/api/billing/minha")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode a2 = parse(minha2);
        assertThat(a2.path("plano").asText()).isEqualTo("PRO");
        assertThat(a2.path("status").asText()).isEqualTo("ATIVA");
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

    private JsonNode parse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
