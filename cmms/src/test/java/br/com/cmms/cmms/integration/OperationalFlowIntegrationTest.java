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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end operational flows at the HTTP boundary:
 * <ul>
 *   <li>Profile: read, update, avatar and password change (relogin with the new one).</li>
 *   <li>2FA: setup → verify → login demands TOTP → login with code → disable.</li>
 *   <li>Work order detail: checklist add/toggle/remove + anexo add/download/remove.</li>
 *   <li>Reports (PDF/XLSX) and the machine QR code endpoint.</li>
 * </ul>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=integration-test-secret-32bytes-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "app.self.url=http://localhost"
})
class OperationalFlowIntegrationTest {

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
    @DisplayName("perfil: ler, atualizar, avatar e troca de senha (relogin com a nova)")
    void profileFlow() throws Exception {
        String token = registrar("Empresa Perfil", "admin-perfil@iso.test");

        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin-perfil@iso.test"));

        String update = objectMapper.createObjectNode()
            .put("nome", "Ana Atualizada").put("telefone", "11999998888")
            .put("cargo", "Gerente").put("departamento", "Manutenção").toString();
        mockMvc.perform(put("/api/profile").header("Authorization", "Bearer " + token)
                .contentType("application/json").content(update))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Ana Atualizada"))
            .andExpect(jsonPath("$.cargo").value("Gerente"));

        mockMvc.perform(post("/api/profile/avatar").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.createObjectNode().put("avatarBase64", "QUJDRA==").toString()))
            .andExpect(status().isOk());

        String troca = objectMapper.createObjectNode()
            .put("senhaAtual", SENHA).put("novaSenha", "NovaSenha123").toString();
        mockMvc.perform(post("/api/profile/password").header("Authorization", "Bearer " + token)
                .contentType("application/json").content(troca))
            .andExpect(status().isOk());

        // Senha antiga cai, nova funciona.
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content(loginJson("admin-perfil@iso.test", SENHA, null)))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content(loginJson("admin-perfil@iso.test", "NovaSenha123", null)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2FA E2E: setup -> verify -> login exige TOTP -> login com codigo -> disable")
    void twoFactorFlow() throws Exception {
        String email = "admin-2fa@iso.test";
        String token = registrar("Empresa 2FA", email);

        MvcResult setup = mockMvc.perform(post("/api/profile/2fa/setup")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.qrCodeDataUri").exists())
            .andReturn();
        String secret = parse(setup).path("secret").asText();
        assertThat(secret).isNotBlank();

        mockMvc.perform(post("/api/profile/2fa/verify").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.createObjectNode().put("code", totp(secret)).toString()))
            .andExpect(status().isOk());

        // Senha correta sem codigo -> TOTP_REQUIRED; com codigo -> tokens.
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content(loginJson(email, SENHA, null)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("TOTP_REQUIRED"));
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content(loginJson(email, SENHA, "000000")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("TOTP_INVALID"));
        MvcResult loginOk = mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content(loginJson(email, SENHA, totp(secret))))
            .andExpect(status().isOk())
            .andReturn();
        String token2 = parse(loginOk).path("accessToken").asText();

        // login-verify pos-login e o disable com senha.
        mockMvc.perform(post("/api/profile/2fa/login-verify").header("Authorization", "Bearer " + token2)
                .contentType("application/json")
                .content(objectMapper.createObjectNode().put("code", totp(secret)).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(delete("/api/profile/2fa").header("Authorization", "Bearer " + token2)
                .contentType("application/json")
                .content(objectMapper.createObjectNode().put("senha", SENHA).toString()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                .content(loginJson(email, SENHA, null)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OS: checklist e anexos via HTTP; relatorios PDF/XLSX; QR da maquina")
    void workOrderReportsAndQr() throws Exception {
        String token = registrar("Empresa Ops", "admin-ops@iso.test");

        // Maquina + OS
        MvcResult maq = mockMvc.perform(post("/api/maquinas").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.createObjectNode()
                    .put("nome", "Torno CNC").put("setor", "Usinagem").put("status", "ATIVO").toString()))
            .andExpect(status().isOk()).andReturn();
        long maquinaId = parse(maq).path("id").asLong();

        MvcResult os = mockMvc.perform(post("/api/manutencoes/" + maquinaId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.createObjectNode()
                    .put("tipo", "PREVENTIVA").put("tecnico", "Ana").toString()))
            .andExpect(status().isOk()).andReturn();
        long osId = parse(os).path("id").asLong();

        // Checklist: add -> toggle -> remove
        MvcResult comItem = mockMvc.perform(post("/api/manutencoes/" + osId + "/checklist")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.createObjectNode().put("descricao", "Trocar filtro").toString()))
            .andExpect(status().isOk()).andReturn();
        long itemId = parse(comItem).path("checklist").get(0).path("id").asLong();

        mockMvc.perform(put("/api/manutencoes/checklist/" + itemId + "/toggle")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.checklist[0].concluido").value(true));

        mockMvc.perform(delete("/api/manutencoes/checklist/" + itemId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.checklist").isEmpty());

        // Anexo: add -> detalhe lista metadados -> download -> remove
        MvcResult comAnexo = mockMvc.perform(post("/api/manutencoes/" + osId + "/anexos")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.createObjectNode()
                    .put("nome", "foto.png").put("contentType", "image/png")
                    .put("dadosBase64", "QUJDRA==").toString()))
            .andExpect(status().isOk()).andReturn();
        long anexoId = parse(comAnexo).path("anexos").get(0).path("id").asLong();

        mockMvc.perform(get("/api/manutencoes/" + osId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anexos[0].nome").value("foto.png"));

        mockMvc.perform(get("/api/manutencoes/anexos/" + anexoId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dadosBase64").value("QUJDRA=="));

        mockMvc.perform(delete("/api/manutencoes/anexos/" + anexoId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anexos").isEmpty());

        // Workflow de status
        mockMvc.perform(put("/api/manutencoes/" + osId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.createObjectNode().put("status", "CONCLUIDA").toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        // Relatorios: PDF e XLSX das tres entidades
        for (String entidade : new String[]{"manutencoes", "maquinas", "estoque"}) {
            for (String formato : new String[]{"pdf", "xlsx"}) {
                MvcResult rel = mockMvc.perform(get("/api/relatorios/" + entidade + "?format=" + formato)
                        .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn();
                assertThat(rel.getResponse().getContentAsByteArray()).isNotEmpty();
            }
        }

        // QR code da maquina (PNG, escopado por empresa)
        MvcResult qr = mockMvc.perform(get("/api/maquinas/" + maquinaId + "/qrcode")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn();
        assertThat(qr.getResponse().getContentType()).isEqualTo("image/png");
        assertThat(qr.getResponse().getContentAsByteArray()).isNotEmpty();
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

    private String loginJson(String email, String senha, String totpCode) {
        var node = objectMapper.createObjectNode().put("email", email).put("senha", senha);
        if (totpCode != null) node.put("totpCode", totpCode);
        return node.toString();
    }

    private JsonNode parse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Independent RFC 6238 code generator (same params as TotpService). */
    private static String totp(String base32Secret) throws Exception {
        byte[] key = br.com.cmms.cmms.service.TotpService.decodeBase32(base32Secret);
        long step = System.currentTimeMillis() / 1000 / 30;
        byte[] msg = ByteBuffer.allocate(8).putLong(step).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(msg);
        int offset = hash[hash.length - 1] & 0x0F;
        int bin = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
        return String.format("%06d", bin % 1_000_000);
    }
}
