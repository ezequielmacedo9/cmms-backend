package br.com.cmms.cmms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, length = 18)
    private String cnpj;

    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(length = 500)
    private String endereco;

    @Enumerated(EnumType.STRING)
    private PlanoAssinatura plano = PlanoAssinatura.STARTER;

    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    private Boolean ativo = true;

    @PrePersist
    protected void onCreate() {
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
        if (ativo == null) ativo = true;
        if (plano == null) plano = PlanoAssinatura.STARTER;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public PlanoAssinatura getPlano() { return plano; }
    public void setPlano(PlanoAssinatura plano) { this.plano = plano; }
    public int getLimiteAtivos() { return plano != null ? plano.getLimiteAtivos() : PlanoAssinatura.STARTER.getLimiteAtivos(); }
    public int getLimiteUsuarios() { return plano != null ? plano.getLimiteUsuarios() : PlanoAssinatura.STARTER.getLimiteUsuarios(); }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public boolean isAtivo() { return Boolean.TRUE.equals(ativo); }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
