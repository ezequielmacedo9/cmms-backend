package br.com.cmms.cmms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PecaRequestDTO {

    @NotBlank
    private  String nome;

    @NotBlank
    private String codigo;

    @NotNull
    @Positive
    private  Integer quantidadeEmEstoque;

    @NotNull
    @Positive
    private Double custoUnitario;

    @NotNull
    @Positive
    private Integer vidaUtilHoras;

    // getters e setters


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

    public Integer getQuantidadeEmEstoque() {
        return quantidadeEmEstoque;
    }

    public void setQuantidadeEmEstoque(Integer quantidadeEmEstoque) {
        this.quantidadeEmEstoque = quantidadeEmEstoque;
    }

    public Double getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(Double custoUnitario) {
        this.custoUnitario = custoUnitario;
    }

    public Integer getVidaUtilHoras() {
        return vidaUtilHoras;
    }

    public void setVidaUtilHoras(Integer vidaUtilHoras) {
        this.vidaUtilHoras = vidaUtilHoras;
    }
}
