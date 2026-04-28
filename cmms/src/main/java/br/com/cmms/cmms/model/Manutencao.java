package br.com.cmms.cmms.model;


import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "manutencoes", indexes = {
    @Index(name = "idx_manutencao_tipo", columnList = "tipo"),
    @Index(name = "idx_manutencao_data", columnList = "data_manutencao"),
    @Index(name = "idx_manutencao_maquina", columnList = "maquina_id"),
    @Index(name = "idx_manutencao_status", columnList = "status")
})
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
    private Long id;

    @Column(nullable = false)
    private String tipo;

    @Column(name = "data_manutencao")
    private LocalDate dataManutencao;

    @Column(length = 20)
    private String prioridade = "MEDIA";

    @Column(length = 20)
    private String status = "ABERTA";

    @Column(name = "prazo_sla")
    private LocalDate prazoSla;

    @Column(name = "data_conclusao")
    private LocalDate dataConclusao;

    @Column(name = "horas_parada")
    private Double horasParada;

    @Column(name = "custo_mao_de_obra")
    private Double custoMaoDeObra = 0.0;

    @Column(name = "observacoes_tecnico", length = 1000)
    private String observacoesTecnico;

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

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDate getPrazoSla() { return prazoSla; }
    public void setPrazoSla(LocalDate prazoSla) { this.prazoSla = prazoSla; }

    public LocalDate getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDate dataConclusao) { this.dataConclusao = dataConclusao; }

    public Double getHorasParada() { return horasParada; }
    public void setHorasParada(Double horasParada) { this.horasParada = horasParada; }

    public Double getCustoMaoDeObra() { return custoMaoDeObra != null ? custoMaoDeObra : 0.0; }
    public void setCustoMaoDeObra(Double custoMaoDeObra) { this.custoMaoDeObra = custoMaoDeObra; }

    public String getObservacoesTecnico() { return observacoesTecnico; }
    public void setObservacoesTecnico(String observacoesTecnico) { this.observacoesTecnico = observacoesTecnico; }

    public boolean isSlaVencido() {
        return prazoSla != null && !"CONCLUIDA".equals(status) && LocalDate.now().isAfter(prazoSla);
    }

    public double calcularCustoTotal() {
        double custosPecas = pecasUtilizadas
                .stream()
                .mapToDouble(Peca::getCustoUnitario)
                .sum();
        return custosPecas + getCustoMaoDeObra();
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
