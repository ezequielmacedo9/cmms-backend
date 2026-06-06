package br.com.cmms.cmms.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Tenant root. Every operational aggregate (Maquina, Manutencao, Peca,
 * Ferramenta), every Usuario and every audit row belongs to exactly one
 * Empresa. Data isolation is enforced by scoping all queries to the caller's
 * {@code empresa_id} — see {@link br.com.cmms.cmms.security.TenantResolver}.
 */
@Entity
@Table(name = "empresa", indexes = {
    @Index(name = "idx_empresa_cnpj", columnList = "cnpj")
})
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 20)
    private String cnpj;

    /** Subscription plan tier. Billing/quota enforcement lands in a later phase. */
    @Column(length = 30, nullable = false)
    private String plano = "TRIAL";

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    void onCreate() {
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
        if (ativo == null) ativo = true;
        if (plano == null) plano = "TRIAL";
    }

    public Empresa() {}

    public Empresa(String nome) {
        this.nome = nome;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getPlano() { return plano; }
    public void setPlano(String plano) { this.plano = plano; }
    public boolean isAtivo() { return !Boolean.FALSE.equals(ativo); }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
