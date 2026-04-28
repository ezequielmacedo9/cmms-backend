package br.com.cmms.cmms.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pecas")
public class Peca {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String codigo;

    private int vidaUtilHoras;

    @Column(nullable = false)
    private int quantidadeEmEstoque;

    @Column(nullable = false)
    private double custoUnitario;

    @Column(name = "quantidade_minima")
    private int quantidadeMinima = 0;

    @ManyToMany(mappedBy = "pecasUtilizadas")
    private List<Manutencao> manutencoes = new ArrayList<>();

    public Peca() {
    }

    public Peca(String nome, int quantidadeEmEstoque) {
        this.nome = nome;
        this.quantidadeEmEstoque = quantidadeEmEstoque;
    }


    // 🔹 GETTERS / SETTERS
    public Long getId() {
        return (Long) id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public int getVidaUtilHoras() {
        return vidaUtilHoras;
    }

    public void setVidaUtilHoras(int vidaUtilHoras) {
        this.vidaUtilHoras = vidaUtilHoras;
    }

    public int getQuantidadeEmEstoque() {
        return quantidadeEmEstoque;
    }

    public void setQuantidadeEmEstoque(int quantidadeEmEstoque) {
        this.quantidadeEmEstoque = quantidadeEmEstoque;
    }

    public double getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(double custoUnitario) {
        this.custoUnitario = custoUnitario;
    }

    public int getQuantidadeMinima() { return quantidadeMinima; }
    public void setQuantidadeMinima(int quantidadeMinima) { this.quantidadeMinima = quantidadeMinima; }

    public boolean isAbaixoDoMinimo() { return quantidadeEmEstoque <= quantidadeMinima; }

    // 🔹 REGRA DE NEGÓCIO (MANTIDA)
    public void baixarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade inválida");
        }
        if (quantidade > quantidadeEmEstoque) {
            throw new IllegalStateException("Estoque insuficiente");
        }
        quantidadeEmEstoque -= quantidade;
    }
}
