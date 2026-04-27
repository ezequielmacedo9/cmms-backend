package br.com.cmms.cmms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracao_sistema")
public class ConfiguracaoSistema {

    @Id
    @Column(length = 100)
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
    public ConfiguracaoSistema(String chave, String valor, String grupo, String tipo, String descricao) {
        this.chave = chave; this.valor = valor; this.grupo = grupo;
        this.tipo = tipo; this.descricao = descricao;
    }

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
