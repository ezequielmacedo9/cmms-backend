package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.AuditLogResponseDTO;
import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.model.AuditLog;
import br.com.cmms.cmms.repository.AuditLogRepository;
import br.com.cmms.cmms.security.TenantResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
@Tag(name = "Auditoria")
public class AuditLogController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 30;

    private final AuditLogRepository repo;
    private final TenantResolver tenant;

    public AuditLogController(AuditLogRepository repo, TenantResolver tenant) {
        this.repo = repo;
        this.tenant = tenant;
    }

    @GetMapping
    @Operation(summary = "Listar trilha de auditoria",
        description = "Ordem cronológica decrescente. Suporta filtro por e-mail (q) e paginação.")
    public ResponseEntity<PagedResponseDTO<AuditLogResponseDTO>> listar(
            @Parameter(description = "Página (0-indexed).")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página (1..100).")
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Filtro parcial por user_email (case-insensitive).")
            @RequestParam(required = false) String q) {

        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), boundedSize);
        Long empresaId = tenant.requireEmpresaId();

        Page<AuditLog> result = (q != null && !q.isBlank())
            ? repo.findByEmpresaIdAndUserEmailContainingIgnoreCaseOrderByTimestampDesc(empresaId, q, pageable)
            : repo.findByEmpresaIdOrderByTimestampDesc(empresaId, pageable);

        return ResponseEntity.ok(PagedResponseDTO.of(result, AuditLogResponseDTO::from));
    }
}
