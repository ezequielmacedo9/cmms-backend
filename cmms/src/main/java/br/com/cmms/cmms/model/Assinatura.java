package br.com.cmms.cmms.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * One subscription per {@link Empresa}. Tracks the chosen {@link Plano}, the
 * lifecycle status and the trial / billing dates. Payment-gateway integration
 * (Asaas/Stripe) lands in a follow-up — for now checkout activates manually.
 */
@Entity
@Table(name = "assinatura", indexes = {
    @Index(name = "uk_assinatura_empresa", columnList = "empresa_id", unique = true)
})
public class Assinatura {

    /** Lifecycle states mirrored by the frontend. */
    public static final String TRIAL        = "TRIAL";
    public static final String ATIVA        = "ATIVA";
    public static final String INADIMPLENTE = "INADIMPLENTE";
    public static final String CANCELADA    = "CANCELADA";

    private static final int TRIAL_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, unique = true)
    private Long empresaId;

    @Column(length = 30, nullable = false)
    private String plano = Plano.STARTER.name();

    @Column(length = 30, nullable = false)
    private String status = TRIAL;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "trial_fim")
    private LocalDate trialFim;

    @Column(name = "data_proxima_cobranca")
    private LocalDate dataProximaCobranca;

    @PrePersist
    void onCreate() {
        LocalDate hoje = LocalDate.now();
        if (dataInicio == null) dataInicio = hoje;
        if (trialFim == null) trialFim = hoje.plusDays(TRIAL_DAYS);
        if (dataProximaCobranca == null) dataProximaCobranca = trialFim;
        if (plano == null) plano = Plano.STARTER.name();
        if (status == null) status = TRIAL;
    }

    public Assinatura() {}

    public Assinatura(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public String getPlano() { return plano; }
    public void setPlano(String plano) { this.plano = plano; }
    public Plano getPlanoEnum() {
        Plano p = Plano.fromNome(plano);
        return p != null ? p : Plano.STARTER;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getTrialFim() { return trialFim; }
    public void setTrialFim(LocalDate trialFim) { this.trialFim = trialFim; }
    public LocalDate getDataProximaCobranca() { return dataProximaCobranca; }
    public void setDataProximaCobranca(LocalDate dataProximaCobranca) { this.dataProximaCobranca = dataProximaCobranca; }

    // ── derived ──────────────────────────────────────────────────────────

    public boolean trialAtivo() {
        return TRIAL.equals(status) && trialFim != null && !LocalDate.now().isAfter(trialFim);
    }

    public int diasTrialRestantes() {
        if (!TRIAL.equals(status) || trialFim == null) return 0;
        long dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), trialFim);
        return (int) Math.max(0, dias);
    }

    /** True when the empresa currently has access (active plan or live trial). */
    public boolean temAcesso() {
        return ATIVA.equals(status) || trialAtivo();
    }
}
