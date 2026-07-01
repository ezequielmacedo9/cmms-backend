package br.com.cmms.cmms.model;

import jakarta.persistence.*;

/** A single checklist line on a work order (e.g. "Trocar óleo", done/not done). */
@Entity
@Table(name = "manutencao_checklist", indexes = {
    @Index(name = "idx_checklist_manutencao", columnList = "manutencao_id"),
    @Index(name = "idx_checklist_empresa", columnList = "empresa_id")
})
public class ManutencaoChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "manutencao_id", nullable = false)
    private Long manutencaoId;

    @Column(nullable = false, length = 300)
    private String descricao;

    @Column(nullable = false)
    private boolean concluido = false;

    public ManutencaoChecklistItem() {}

    public ManutencaoChecklistItem(Long empresaId, Long manutencaoId, String descricao) {
        this.empresaId = empresaId;
        this.manutencaoId = manutencaoId;
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public Long getManutencaoId() { return manutencaoId; }
    public void setManutencaoId(Long manutencaoId) { this.manutencaoId = manutencaoId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isConcluido() { return concluido; }
    public void setConcluido(boolean concluido) { this.concluido = concluido; }
}
