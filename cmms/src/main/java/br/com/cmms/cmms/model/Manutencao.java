package br.com.cmms.cmms.model;


import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "manutencoes", indexes = {
    @Index(name = "idx_manutencao_tipo", columnList = "tipo"),
    @Index(name = "idx_manutencao_data", columnList = "data_manutencao"),
    @Index(name = "idx_manutencao_maquina", columnList = "maquina_id"),
    @Index(name = "idx_manutencao_status", columnList = "status")
})
@SQLRestriction("deleted_at IS NULL")
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

    /** Tenant owner. Every query is scoped to this column. */
    @Column(name = "empresa_id")
    private Long empresaId;

    @ManyToOne
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(nullable = false)
    private String tecnico;

    /** Optional link to the responsible user (técnico = usuário). */
    @Column(name = "tecnico_id")
    private Long tecnicoId;

    @Column(length = 500)
    private String descricao;

    /** Labor time in minutes (for cost/throughput metrics). */
    @Column(name = "tempo_execucao_minutos")
    private Integer tempoExecucaoMinutos;

    /** Labor cost for this work order. Parts cost lives in ManutencaoPeca. */
    @Column(name = "custo_mao_obra")
    private Double custoMaoObra;

    /** When the work order was opened (for MTTR = conclusao - abertura). */
    @Column(name = "data_abertura")
    private LocalDate dataAbertura;

    @Column(name = "data_conclusao")
    private LocalDate dataConclusao;

    @ManyToMany
    @JoinTable(
            name = "manutencao_pecas",
            joinColumns = @JoinColumn(name = "manutencao_id"),
            inverseJoinColumns = @JoinColumn(name = "peca_id")
    )
    private List<Peca> pecasUtilizadas = new ArrayList<>();

    /** Soft-delete marker. See class-level {@code @SQLRestriction}. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 🔹 CONSTRUTOR VAZIO (JPA)
    public Manutencao() {
    }

    // 🔹 GETTERS / SETTERS
    public Long getId() {
        return id;
    }

    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }

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

    public Long getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Long tecnicoId) { this.tecnicoId = tecnicoId; }

    public Integer getTempoExecucaoMinutos() { return tempoExecucaoMinutos; }
    public void setTempoExecucaoMinutos(Integer tempoExecucaoMinutos) { this.tempoExecucaoMinutos = tempoExecucaoMinutos; }

    public Double getCustoMaoObra() { return custoMaoObra; }
    public void setCustoMaoObra(Double custoMaoObra) { this.custoMaoObra = custoMaoObra; }

    public LocalDate getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDate dataAbertura) { this.dataAbertura = dataAbertura; }

    public LocalDate getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDate dataConclusao) { this.dataConclusao = dataConclusao; }

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

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public boolean isDeleted() { return deletedAt != null; }

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
