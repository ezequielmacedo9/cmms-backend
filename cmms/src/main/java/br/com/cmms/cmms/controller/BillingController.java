package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.AssinaturaInfoDTO;
import br.com.cmms.cmms.dto.CheckoutResultDTO;
import br.com.cmms.cmms.dto.PlanoInfoDTO;
import br.com.cmms.cmms.security.TenantResolver;
import br.com.cmms.cmms.service.AssinaturaService;
import br.com.cmms.cmms.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * Subscription / billing API consumed by the frontend's "Assinatura" page.
 * Read endpoints are available to any authenticated user of the empresa;
 * mutations are restricted to ADMIN / SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/billing")
@Tag(name = "Assinatura")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final AssinaturaService assinaturaService;
    private final PagamentoService pagamentoService;
    private final TenantResolver tenant;

    public BillingController(AssinaturaService assinaturaService, PagamentoService pagamentoService,
                            TenantResolver tenant) {
        this.assinaturaService = assinaturaService;
        this.pagamentoService = pagamentoService;
        this.tenant = tenant;
    }

    @GetMapping("/planos")
    @Operation(summary = "Catálogo de planos (limites + preço)")
    public ResponseEntity<Map<String, PlanoInfoDTO>> planos() {
        return ResponseEntity.ok(assinaturaService.planos());
    }

    @GetMapping("/minha")
    @Operation(summary = "Assinatura da empresa do usuário autenticado")
    public ResponseEntity<AssinaturaInfoDTO> minha() {
        return ResponseEntity.ok(assinaturaService.minha(tenant.requireEmpresaId()));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Assinar um plano",
        description = "Sem gateway de pagamento ainda: ativa o plano e devolve linkPagamento vazio.")
    public ResponseEntity<CheckoutResultDTO> checkout(@Valid @RequestBody PlanoRequest body) {
        return ResponseEntity.ok(assinaturaService.checkout(tenant.requireEmpresaId(), body.plano()));
    }

    @PutMapping("/upgrade")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Alterar o plano (upgrade/downgrade)")
    public ResponseEntity<AssinaturaInfoDTO> upgrade(@Valid @RequestBody PlanoRequest body) {
        return ResponseEntity.ok(assinaturaService.upgrade(tenant.requireEmpresaId(), body.plano()));
    }

    @PostMapping("/cancelar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Cancelar a assinatura")
    public ResponseEntity<Void> cancelar() {
        assinaturaService.cancelar(tenant.requireEmpresaId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/webhook")
    @SecurityRequirements
    @Operation(summary = "Webhook de pagamento (Asaas)",
        description = "Recebido pelo gateway. Em PAYMENT_CONFIRMED/RECEIVED ativa a assinatura "
                    + "da empresa (externalReference). Protegido por token compartilhado.")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "asaas-access-token", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        if (!pagamentoService.webhookAutorizado(token)) {
            return ResponseEntity.status(401).build();
        }
        String event = String.valueOf(payload.get("event"));
        Object pgto = payload.get("payment");
        if (("PAYMENT_CONFIRMED".equals(event) || "PAYMENT_RECEIVED".equals(event))
                && pgto instanceof Map<?, ?> payment) {
            Object ref = ((Map<String, Object>) payment).get("externalReference");
            if (ref != null) {
                try {
                    assinaturaService.confirmarPagamento(Long.parseLong(ref.toString()));
                } catch (NumberFormatException e) {
                    log.warn("Webhook com externalReference inválido: {}", ref);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    public record PlanoRequest(@NotBlank String plano) {}
}
