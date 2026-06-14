package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.AssinaturaInfoDTO;
import br.com.cmms.cmms.dto.CheckoutResultDTO;
import br.com.cmms.cmms.dto.PlanoInfoDTO;
import br.com.cmms.cmms.security.TenantResolver;
import br.com.cmms.cmms.service.AssinaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private final AssinaturaService assinaturaService;
    private final TenantResolver tenant;

    public BillingController(AssinaturaService assinaturaService, TenantResolver tenant) {
        this.assinaturaService = assinaturaService;
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

    public record PlanoRequest(@NotBlank String plano) {}
}
