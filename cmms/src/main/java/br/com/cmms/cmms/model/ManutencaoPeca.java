package br.com.cmms.cmms.model;

import jakarta.persistence.*;

/**
 * A part consumed by a work order, with the quantity used and a snapshot of
 * the unit cost at consumption time (so historical cost reports stay accurate
 * even if the part's price later changes). Stock is decremented when the row
 * is created — see ManutencaoService.
 */
@Entity
@Table(name = "manutencao_peca", indexes = {
    @Index(name = "idx_mpeca_manutencao", columnList = "manutencao_id"),
    @Index(name = "idx_mpeca_empresa", columnList = "empresa_id")
})
public class ManutencaoPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "manutencao_id", nullable = false)
    private Long manutencaoId;

    @Column(name = "peca_id", nullable = false)
    private Long pecaId;

    /** Snapshot of the part name for reporting without a join. */
    @Column(name = "peca_nome")
    private String pecaNome;

    @Column(nullable = false)
    private int quantidade;

    /** Snapshot of the unit cost at the moment of consumption. */
    @Column(name = "custo_unitario", nullable = false)
    private double custoUnitario;

    public ManutencaoPeca() {}

    public ManutencaoPeca(Long empresaId, Long manutencaoId, Long pecaId, String pecaNome,
                          int quantidade, double custoUnitario) {
        this.empresaId = empresaId;
        this.manutencaoId = manutencaoId;
        this.pecaId = pecaId;
        this.pecaNome = pecaNome;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public Long getManutencaoId() { return manutencaoId; }
    public void setManutencaoId(Long manutencaoId) { this.manutencaoId = manutencaoId; }
    public Long getPecaId() { return pecaId; }
    public void setPecaId(Long pecaId) { this.pecaId = pecaId; }
    public String getPecaNome() { return pecaNome; }
    public void setPecaNome(String pecaNome) { this.pecaNome = pecaNome; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public double getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(double custoUnitario) { this.custoUnitario = custoUnitario; }

    public double subtotal() { return quantidade * custoUnitario; }
}
