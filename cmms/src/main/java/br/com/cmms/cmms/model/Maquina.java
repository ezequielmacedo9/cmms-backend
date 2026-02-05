package br.com.cmms.cmms.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "maquinas")
public class Maquina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String setor;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataUltimaManutencao = LocalDate.now();

    @Column(nullable = false)
    private String status;

    private Integer intervaloPreventivaDias = 0;


    @OneToMany(
            mappedBy = "maquina",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<Manutencao> listaDeManutencoes = new ArrayList<>();

    // 🔹 CONSTRUTORES
    public Maquina() {
    }

    public Maquina(String nome, String setor, String status) {
        this.nome = nome;
        this.setor = setor;
        this.status = status;
    }

    // 🔹 GETTERS / SETTERS

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }

    public LocalDate getDataUltimaManutencao() {
        return dataUltimaManutencao;
    }

    public void setDataUltimaManutencao(LocalDate dataUltimaManutencao) {
        this.dataUltimaManutencao = dataUltimaManutencao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getIntervaloPreventivaDias() {
        return intervaloPreventivaDias;
    }

    public void setIntervaloPreventivaDias(int intervaloPreventivaDias) {
        this.intervaloPreventivaDias = intervaloPreventivaDias;
    }

    public List<Manutencao> getListaDeManutencoes() {
        return listaDeManutencoes;
    }


    public void adicionarManutencao(Manutencao manutencao) {
        if (manutencao != null) {
            manutencao.setMaquina(this);
            listaDeManutencoes.add(manutencao);
        }
    }

    public void removerManutencao(Manutencao manutencao) {
        if (manutencao != null) {
            listaDeManutencoes.remove(manutencao);
            manutencao.setMaquina(null);
        }
    }

    public double calcularCustoTotalManutencoes() {
        return listaDeManutencoes.stream()
                .mapToDouble(Manutencao::calcularCustoTotal)
                .sum();
    }
}

