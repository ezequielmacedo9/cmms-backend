package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.AuditLog;
import br.com.cmms.cmms.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    @Async
    public void log(String userEmail, Long userId, String acao, String recurso,
                    Long recursoId, String detalhes, String ip) {
        repo.save(new AuditLog(userEmail, userId, acao, recurso, recursoId, detalhes, ip));
    }

    @Async
    public void log(String userEmail, Long userId, String acao, String recurso, String detalhes, String ip) {
        log(userEmail, userId, acao, recurso, null, detalhes, ip);
    }

    public static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
