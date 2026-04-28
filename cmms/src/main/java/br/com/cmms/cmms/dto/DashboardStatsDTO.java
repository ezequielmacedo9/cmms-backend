package br.com.cmms.cmms.dto;

import java.util.List;

public record DashboardStatsDTO(
    long totalMaquinas,
    long totalManutencoes,
    long totalPecas,
    long maquinasAtivas,
    long maquinasInativas,
    long maquinasEmManutencao,
    long manutencoesPreventivas,
    long manutencoesCorretivas,
    long manutencoesVencidas,
    double disponibilidade,
    double mtbfDias,
    double mttrHoras,
    double custoTotalMes,
    long totalEstoqueBaixo,
    long manutencoesSlaVencido,
    List<MonthlyCount> ultimosSeisMeses,
    List<OverdueAlert> alertasVencidos
) {
    public record MonthlyCount(int ano, int mes, String label, long total) {}
    public record OverdueAlert(Long maquinaId, String maquinaNome, String setor, long diasVencido, String prioridade) {}
}
