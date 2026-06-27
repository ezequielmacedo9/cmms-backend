package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Plano;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Payment-gateway integration (Asaas). Deliberately a no-op until
 * {@code ASAAS_API_KEY} is configured — without a key {@link #criarLinkPagamento}
 * returns an empty string and the subscription is activated manually (the MVP
 * behaviour). With a key it asks Asaas for a recurring payment link; a webhook
 * later confirms the payment (see BillingController).
 *
 * <p>The Asaas request shape here is a thin scaffold; wiring a production
 * account means verifying the exact endpoint/fields against Asaas docs.
 */
@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final RestTemplate restTemplate;

    @Value("${asaas.api-key:}")
    private String apiKey;

    @Value("${asaas.base-url:https://sandbox.asaas.com/api/v3}")
    private String baseUrl;

    @Value("${asaas.webhook-token:}")
    private String webhookToken;

    public PagamentoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean habilitado() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Returns a payment link, or empty string when the gateway is disabled / fails. */
    public String criarLinkPagamento(Long empresaId, Plano plano) {
        if (!habilitado()) return "";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("access_token", apiKey);

            Map<String, Object> body = Map.of(
                "name", "CMMS - Plano " + plano.name(),
                "billingType", "UNDEFINED",
                "chargeType", "RECURRENT",
                "subscriptionCycle", "MONTHLY",
                "value", plano.getValorMensal(),
                "externalReference", String.valueOf(empresaId)
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                baseUrl + "/paymentLinks", new HttpEntity<>(body, headers), Map.class);
            Object url = resp != null ? resp.get("url") : null;
            return url != null ? url.toString() : "";
        } catch (Exception e) {
            log.warn("Falha ao criar link de pagamento Asaas (empresa={}): {}", empresaId, e.getMessage());
            return "";
        }
    }

    /** Validates the webhook shared token when one is configured. */
    public boolean webhookAutorizado(String token) {
        if (webhookToken == null || webhookToken.isBlank()) return true; // dev / not configured
        return webhookToken.equals(token);
    }
}
