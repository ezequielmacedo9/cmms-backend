package br.com.cmms.cmms.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "manutencoes")
public class Manutencao {

    @ManyToMany
    @JoinTable(
            name = "manutencao_ferramentas",
            joinColumns = @JoinColumn(name = "manutencao_id"),
            inverseJoinColumns = @JoinColumn(name = "ferramenta_id")
    )
    private List<Ferramenta> ferramentasUtilizadas = new ArrayList<>();


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String tipo;

    @JsonProperty("data")
    private LocalDate dataManutencao;

    @ManyToOne
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(nullable = false)
    private String tecnico;

    @Column(length = 500)
    private String descricao;

    @ManyToMany
    @JoinTable(
            name = "manutencao_pecas",
            joinColumns = @JoinColumn(name = "manutencao_id"),
            inverseJoinColumns = @JoinColumn(name = "peca_id")
    )
    private List<Peca> pecasUtilizadas = new ArrayList<>();

    // 🔹 CONSTRUTOR VAZIO (JPA)
    public Manutencao() {
    }

    // 🔹 GETTERS / SETTERS
    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getDataManutencao() {
        return dataManutencao;
    }

    public void setDataManutencao(LocalDate dataManutencao) {
        this.dataManutencao = dataManutencao;
    }

    public Maquina getMaquina() {
        return maquina;
    }

    public void setMaquina(Maquina maquina) {
        this.maquina = maquina;
    }

    public String getTecnico() {
        return tecnico;
    }

    public void setTecnico(String tecnico) {
        this.tecnico = tecnico;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public List<Peca> getPecasUtilizadas() {
        return pecasUtilizadas;
    }

    // 🔹 MÉTODOS DE NEGÓCIO
    public void adicionarPeca(Peca peca) {
        if (peca != null) {
            pecasUtilizadas.add(peca);
        }
    }

    public double calcularCustoTotal() {
        return pecasUtilizadas
                .stream()
                .mapToDouble(Peca::getCustoUnitario)
                .sum();
    }

    @Override
    public String toString() {
        return "Manutenção: " + tipo +
                " | Máquina: " + maquina.getNome() +
                " | Data: " + dataManutencao;
    }

    public void adicionarFerramentas(Ferramenta ferramenta){
        ferramentasUtilizadas.add(ferramenta);
    }

    public List<Ferramenta> getFerramentasUtilizadas(){
        return ferramentasUtilizadas;
    }
}
