package br.com.cmms.cmms.model;

import jakarta.persistence.*;

/**
 * Per-tenant key/value setting. Each empresa owns its own copy of the
 * default catalog (seeded lazily by {@code ConfiguracaoService}); the pair
 * (empresa_id, chave) is unique.
 */
@Entity
@Table(name = "configuracao_sistema",
       uniqueConstraints = @UniqueConstraint(name = "uq_config_empresa_chave",
                                             columnNames = {"empresa_id", "chave"}))
public class ConfiguracaoSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(length = 100, nullable = false)
    private String chave;

    @Column(columnDefinition = "TEXT")
    private String valor;

    @Column(length = 50)
    private String grupo;

    @Column(length = 20)
    private String tipo; // STRING, BOOLEAN, NUMBER, JSON

    @Column(length = 200)
    private String descricao;

    public ConfiguracaoSistema() {}

    public ConfiguracaoSistema(Long empresaId, String chave, String valor, String grupo, String tipo, String descricao) {
        this.empresaId = empresaId; this.chave = chave; this.valor = valor;
        this.grupo = grupo; this.tipo = tipo; this.descricao = descricao;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
