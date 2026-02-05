package br.com.cmms.cmms.dto;

import br.com.cmms.cmms.model.Peca;

public class PecaResponseDTO {

    private Long id;
    private String nome;
    private String codigo;
    private Integer quantidadeEmEstoque;
    private Double custoUnitario;
    private Integer vidaUtilHoras;

    public PecaResponseDTO() {
    }

    public PecaResponseDTO(Peca peca) {
        this.id = peca.getId();
        this.nome = peca.getNome();
        this.codigo = peca.getCodigo();
        this.quantidadeEmEstoque = peca.getQuantidadeEmEstoque();
        this.custoUnitario = peca.getCustoUnitario();
        this.vidaUtilHoras = peca.getVidaUtilHoras();
    }


    // getters e setters

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
