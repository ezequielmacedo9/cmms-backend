package br.com.cmms.cmms.dto;

import br.com.cmms.cmms.model.AuditLog;

import java.time.LocalDateTime;

/**
 * Public projection of an audit entry. Excludes any internal field that
 * might be added later (private flags, raw payloads, etc.).
 */
public record AuditLogResponseDTO(
    Long id,
    String userEmail,
    Long userId,
    String acao,
    String recurso,
    Long recursoId,
    String detalhes,
    String ip,
    LocalDateTime timestamp
) {

    public static AuditLogResponseDTO from(AuditLog a) {
        return new AuditLogResponseDTO(
            a.getId(),
            a.getUserEmail(),
            a.getUserId(),
            a.getAcao(),
            a.getRecurso(),
            a.getRecursoId(),
            a.getDetalhes(),
            a.getIp(),
            a.getTimestamp()
        );
    }
}
