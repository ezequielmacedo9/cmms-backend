package br.com.cmms.cmms.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "ferramentas")
public class Ferramenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String nome;
    private String codigo;
    private String status;
    private String localizacao;
    private String responsavel;
    private LocalDate dataUltimaManutencao;


    public Ferramenta(){
    }

    public Ferramenta(String nome) {
        this.nome = nome;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDataUltimaManutencao(LocalDate dataUltimaManutencao) {
        this.dataUltimaManutencao = dataUltimaManutencao;
    }


    public String getNome() {
        return nome;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getStatus() {
        return status;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public LocalDate getDataUltimaManutencao() {
        return dataUltimaManutencao;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    @ManyToMany(mappedBy = "ferramentasUtilizadas")
    private List<Manutencao> manutencoes = new ArrayList<>();
}