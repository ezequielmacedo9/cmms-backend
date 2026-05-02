package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.PlanoAssinatura;
import br.com.cmms.cmms.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final BillingService billingService;

    @Value("${asaas.webhook.token:}")
    private String webhookToken;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/planos")
    public ResponseEntity<Map<String, Object>> listarPlanos() {
        return ResponseEntity.ok(billingService.listarPlanos());
    }

    @GetMapping("/minha")
    public ResponseEntity<Map<String, Object>> getMinhaAssinatura() {
        return ResponseEntity.ok(billingService.getMinhaAssinatura());
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody Map<String, String> body) {
        PlanoAssinatura plano = PlanoAssinatura.valueOf(body.getOrDefault("plano", "STARTER").toUpperCase());
        return ResponseEntity.ok(billingService.checkout(plano));
    }

    @PutMapping("/upgrade")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> upgrade(@RequestBody Map<String, String> body) {
        PlanoAssinatura novoPlano = PlanoAssinatura.valueOf(body.getOrDefault("plano", "PRO").toUpperCase());
        return ResponseEntity.ok(billingService.upgrade(novoPlano));
    }

    @PostMapping("/cancelar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> cancelar() {
        billingService.cancelar();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "asaas-access-token", required = false) String token,
            @RequestBody String payload) {

        if (webhookToken != null && !webhookToken.isBlank()) {
            if (!webhookToken.equals(token)) {
                log.warn("Webhook Asaas rejeitado: token inválido ou ausente");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } else {
            log.warn("ASAAS_WEBHOOK_TOKEN não configurado — webhook aceito sem autenticação");
        }

        billingService.processarWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
