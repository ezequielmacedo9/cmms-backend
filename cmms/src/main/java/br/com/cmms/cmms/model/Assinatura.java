package br.com.cmms.cmms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assinaturas")
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", unique = true)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    private PlanoAssinatura plano;

    private BigDecimal valorMensal;

    private LocalDate dataInicio;
    private LocalDate dataProximaCobranca;

    @Enumerated(EnumType.STRING)
    private StatusAssinatura status = StatusAssinatura.TRIAL;

    private Integer diasTrial = 14;

    @Column(length = 100)
    private String gatewayClienteId;

    @Column(length = 100)
    private String gatewayAssinaturaId;

    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
        if (status == null) status = StatusAssinatura.TRIAL;
        if (diasTrial == null) diasTrial = 14;
        if (dataInicio == null) dataInicio = LocalDate.now();
        if (dataProximaCobranca == null && diasTrial != null) {
            dataProximaCobranca = dataInicio.plusDays(diasTrial);
        }
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public PlanoAssinatura getPlano() { return plano; }
    public void setPlano(PlanoAssinatura plano) { this.plano = plano; }
    public BigDecimal getValorMensal() { return valorMensal; }
    public void setValorMensal(BigDecimal valorMensal) { this.valorMensal = valorMensal; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataProximaCobranca() { return dataProximaCobranca; }
    public void setDataProximaCobranca(LocalDate dataProximaCobranca) { this.dataProximaCobranca = dataProximaCobranca; }
    public StatusAssinatura getStatus() { return status; }
    public void setStatus(StatusAssinatura status) { this.status = status; }
    public Integer getDiasTrial() { return diasTrial; }
    public void setDiasTrial(Integer diasTrial) { this.diasTrial = diasTrial; }
    public String getGatewayClienteId() { return gatewayClienteId; }
    public void setGatewayClienteId(String id) { this.gatewayClienteId = id; }
    public String getGatewayAssinaturaId() { return gatewayAssinaturaId; }
    public void setGatewayAssinaturaId(String id) { this.gatewayAssinaturaId = id; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }

    public boolean isTrialAtivo() {
        return status == StatusAssinatura.TRIAL
            && dataProximaCobranca != null
            && !LocalDate.now().isAfter(dataProximaCobranca);
    }

    public boolean isAcesso() {
        return status == StatusAssinatura.TRIAL && isTrialAtivo()
            || status == StatusAssinatura.ATIVA;
    }
}
