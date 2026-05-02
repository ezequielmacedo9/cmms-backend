package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Empresa;
import br.com.cmms.cmms.model.PlanoAssinatura;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class AsaasService {

    private static final Logger log = LoggerFactory.getLogger(AsaasService.class);
    private static final String ASAAS_BASE = "https://api.asaas.com/v3";

    private static final Map<PlanoAssinatura, BigDecimal> PRECOS = Map.of(
        PlanoAssinatura.STARTER,    new BigDecimal("297.00"),
        PlanoAssinatura.PRO,        new BigDecimal("597.00"),
        PlanoAssinatura.BUSINESS,   new BigDecimal("997.00"),
        PlanoAssinatura.ENTERPRISE, new BigDecimal("1497.00")
    );

    @Value("${asaas.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AsaasService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public BigDecimal getPreco(PlanoAssinatura plano) {
        return PRECOS.getOrDefault(plano, BigDecimal.ZERO);
    }

    public Map<PlanoAssinatura, BigDecimal> getTodosPrecos() {
        return PRECOS;
    }

    /** Create a customer in Asaas. Returns customer ID or null on failure/unconfigured. */
    public String criarCliente(Empresa empresa) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Asaas API key not configured — skipping customer creation");
            return null;
        }
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("name", empresa.getNome());
            body.put("cpfCnpj", empresa.getCnpj());
            if (empresa.getEmail() != null) body.put("email", empresa.getEmail());
            if (empresa.getTelefone() != null) body.put("phone", empresa.getTelefone());

            JsonNode resp = post("/customers", body);
            String id = resp.path("id").asText(null);
            log.info("Asaas customer created: {} for empresa {}", id, empresa.getId());
            return id;
        } catch (Exception e) {
            log.error("Failed to create Asaas customer for empresa {}: {}", empresa.getId(), e.getMessage());
            return null;
        }
    }

    /** Create a recurring subscription. Returns subscription ID or null on failure. */
    public String criarAssinatura(String gatewayClienteId, PlanoAssinatura plano, String nextDue) {
        if (apiKey == null || apiKey.isBlank() || gatewayClienteId == null) return null;
        try {
            BigDecimal valor = getPreco(plano);
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("customer", gatewayClienteId);
            body.put("billingType", "UNDEFINED"); // PIX + boleto + cartão
            body.put("value", valor);
            body.put("nextDueDate", nextDue);
            body.put("cycle", "MONTHLY");
            body.put("description", "CMMS Industrial Suite — Plano " + plano.name());

            JsonNode resp = post("/subscriptions", body);
            String id = resp.path("id").asText(null);
            log.info("Asaas subscription created: {}", id);
            return id;
        } catch (Exception e) {
            log.error("Failed to create Asaas subscription: {}", e.getMessage());
            return null;
        }
    }

    /** Generate a payment link (checkout URL) for a subscription. */
    public String gerarLinkCheckout(String gatewayAssinaturaId) {
        if (apiKey == null || apiKey.isBlank() || gatewayAssinaturaId == null) return null;
        try {
            JsonNode resp = get("/subscriptions/" + gatewayAssinaturaId + "/paymentLink");
            return resp.path("paymentLink").asText(null);
        } catch (Exception e) {
            log.warn("Failed to get payment link: {}", e.getMessage());
            return null;
        }
    }

    /** Cancel a subscription. */
    public boolean cancelarAssinatura(String gatewayAssinaturaId) {
        if (apiKey == null || apiKey.isBlank() || gatewayAssinaturaId == null) return false;
        try {
            delete("/subscriptions/" + gatewayAssinaturaId);
            return true;
        } catch (Exception e) {
            log.error("Failed to cancel Asaas subscription {}: {}", gatewayAssinaturaId, e.getMessage());
            return false;
        }
    }

    // ── HTTP helpers ────────────────────────────────────────────────────────

    private JsonNode post(String path, Object body) throws Exception {
        HttpHeaders headers = headers();
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(ASAAS_BASE + path, entity, String.class);
        return objectMapper.readTree(resp.getBody());
    }

    private JsonNode get(String path) throws Exception {
        HttpHeaders headers = headers();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(ASAAS_BASE + path, HttpMethod.GET, entity, String.class);
        return objectMapper.readTree(resp.getBody());
    }

    private void delete(String path) {
        HttpHeaders headers = headers();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(ASAAS_BASE + path, HttpMethod.DELETE, entity, String.class);
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("access_token", apiKey);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
