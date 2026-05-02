package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.PlanoAssinatura;
import br.com.cmms.cmms.service.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

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
    public ResponseEntity<Void> webhook(@RequestBody String payload) {
        billingService.processarWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
