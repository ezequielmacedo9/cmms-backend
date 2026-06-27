package br.com.cmms.cmms.dto;

import java.util.List;

/**
 * Management dashboard a director wants on Monday morning. Only KPIs that the
 * current data model supports are exposed here; cost-per-asset, MTTR and
 * availability depend on richer work-order data (labor time, downtime, parts
 * consumed) that arrives with the Work Order phase.
 */
public record RelatorioGerencialDTO(
    long totalMaquinas,
    long totalManutencoes,
    long manutencoesPreventivas,
    long manutencoesCorretivas,
    long maquinasComPreventiva,
    long preventivasVencidas,
    double cumprimentoPreventivaPct,
    double disponibilidade,
    double mtbfDias,
    double mttrDias,
    double custoTotalManutencoes,
    double valorTotalEstoque,
    List<Ofensor> topOfensores
) {
    /** A machine ranked by how many corrective maintenances it required. */
    public record Ofensor(Long maquinaId, String maquinaNome, long corretivas) {}
}
