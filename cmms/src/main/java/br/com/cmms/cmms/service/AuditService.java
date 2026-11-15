package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.AuditLog;
import br.com.cmms.cmms.repository.AuditLogRepository;
import br.com.cmms.cmms.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    @Async
    public void log(Long empresaId, String userEmail, Long userId, String acao, String recurso,
                    Long recursoId, String detalhes, String ip) {
        repo.save(new AuditLog(empresaId, userEmail, userId, acao, recurso, recursoId, detalhes, ip));
    }

    @Async
    public void log(Long empresaId, String userEmail, Long userId, String acao, String recurso, String detalhes, String ip) {
        log(empresaId, userEmail, userId, acao, recurso, null, detalhes, ip);
    }

    /** Client IP recorded in the audit trail; same source of truth as the rate limiter. */
    public static String getClientIp(HttpServletRequest request) {
        return ClientIpResolver.resolve(request);
    }
}
