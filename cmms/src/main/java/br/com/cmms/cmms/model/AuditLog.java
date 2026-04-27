package br.com.cmms.cmms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_email"),
    @Index(name = "idx_audit_ts",   columnList = "timestamp")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 100, nullable = false)
    private String acao;

    @Column(length = 100)
    private String recurso;

    @Column(name = "recurso_id")
    private Long recursoId;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @Column(length = 50)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() { if (timestamp == null) timestamp = LocalDateTime.now(); }

    public AuditLog() {}
    public AuditLog(String userEmail, Long userId, String acao, String recurso, Long recursoId, String detalhes, String ip) {
        this.userEmail = userEmail; this.userId = userId;
        this.acao = acao; this.recurso = recurso; this.recursoId = recursoId;
        this.detalhes = detalhes; this.ip = ip;
    }

    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getRecurso() { return recurso; }
    public void setRecurso(String recurso) { this.recurso = recurso; }
    public Long getRecursoId() { return recursoId; }
    public void setRecursoId(Long recursoId) { this.recursoId = recursoId; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
