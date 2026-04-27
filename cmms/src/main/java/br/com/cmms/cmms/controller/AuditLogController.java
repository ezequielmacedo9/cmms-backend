package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.AuditLog;
import br.com.cmms.cmms.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AuditLogController {

    private final AuditLogRepository repo;

    public AuditLogController(AuditLogRepository repo) { this.repo = repo; }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String q) {
        size = Math.min(size, 100);
        Page<AuditLog> result = (q != null && !q.isBlank())
            ? repo.findByUserEmailContainingIgnoreCaseOrderByTimestampDesc(q, PageRequest.of(page, size))
            : repo.findAllByOrderByTimestampDesc(PageRequest.of(page, size));

        return ResponseEntity.ok(Map.of(
            "content",       result.getContent(),
            "totalElements", result.getTotalElements(),
            "totalPages",    result.getTotalPages(),
            "page",          page,
            "size",          size
        ));
    }
}
