package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.ConfiguracaoSistemaDTO;
import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.security.TenantResolver;
import br.com.cmms.cmms.service.AuditService;
import br.com.cmms.cmms.service.ConfiguracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracoes")
@Tag(name = "Configurações")
public class ConfiguracaoController {

    private final ConfiguracaoService service;
    private final AuditService audit;
    private final TenantResolver tenant;

    public ConfiguracaoController(ConfiguracaoService service, AuditService audit, TenantResolver tenant) {
        this.service = service;
        this.audit = audit;
        this.tenant = tenant;
    }

    /**
     * Listing supports two response shapes:
     *   - paginado: Page&lt;ConfiguracaoSistemaDTO&gt; (default)
     *   - completo: List&lt;ConfiguracaoSistemaDTO&gt; quando {@code ?unpaged=true}
     */
    @GetMapping
    @Operation(summary = "Listar configurações do sistema",
        description = "Suporta paginação (page, size, sort), busca (q) e filtro por grupo.")
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String grupo,
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 50, sort = "chave", direction = Sort.Direction.ASC) Pageable pageable) {
        if (unpaged) {
            List<ConfiguracaoSistemaDTO> all = service.listar().stream()
                .map(ConfiguracaoSistemaDTO::from)
                .toList();
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(
            PagedResponseDTO.of(service.listar(q, grupo, pageable), ConfiguracaoSistemaDTO::from));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Atualizar configurações em lote",
        description = "Recebe um Map<chave, valor>. Apenas chaves pré-existentes são atualizadas.")
    public ResponseEntity<Map<String, String>> salvar(
            @RequestBody Map<String, String> values,
            @AuthenticationPrincipal UserDetails ud,
            HttpServletRequest request) {
        service.salvar(values);
        audit.log(tenant.empresaIdAtual(), ud.getUsername(), null, "CONFIG_UPDATE", "CONFIGURACAO", null,
            "Configurações atualizadas: " + values.keySet(),
            AuditService.getClientIp(request));
        return ResponseEntity.ok(Map.of("message", "Configurações salvas"));
    }
}
