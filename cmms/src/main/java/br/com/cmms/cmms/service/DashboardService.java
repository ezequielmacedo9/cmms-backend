package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.DashboardStatsDTO;
import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PecaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final String[] MESES_PT = {"Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"};

    private final MaquinaRepository maquinaRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final PecaRepository pecaRepository;

    public DashboardService(MaquinaRepository maquinaRepository,
                            ManutencaoRepository manutencaoRepository,
                            PecaRepository pecaRepository) {
        this.maquinaRepository = maquinaRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.pecaRepository = pecaRepository;
    }

    @Cacheable("dashboard-stats")
    public DashboardStatsDTO getStats() {
        log.info("Computando estatísticas do dashboard");

        List<Maquina> maquinas = maquinaRepository.findAll();
        List<Manutencao> manutencoes = manutencaoRepository.findAll();
        long totalPecas = pecaRepository.count();

        long totalMaquinas = maquinas.size();
        long maquinasAtivas = maquinas.stream().filter(m -> "ATIVO".equals(m.getStatus())).count();
        long maquinasInativas = maquinas.stream().filter(m -> "INATIVO".equals(m.getStatus())).count();
        long maquinasEmManutencao = maquinas.stream().filter(m -> "EM_MANUTENCAO".equals(m.getStatus())).count();

        long manutencoesPreventivas = manutencoes.stream().filter(m -> "PREVENTIVA".equals(m.getTipo())).count();
        long manutencoesCorretivas = manutencoes.stream().filter(m -> "CORRETIVA".equals(m.getTipo())).count();

        LocalDate hoje = LocalDate.now();
        long manutencoesVencidas = maquinas.stream()
            .filter(m -> m.getIntervaloPreventivaDias() != null && m.getIntervaloPreventivaDias() > 0)
            .filter(m -> {
                if (m.getDataUltimaManutencao() == null) return true;
                return m.getDataUltimaManutencao().plusDays(m.getIntervaloPreventivaDias()).isBefore(hoje);
            })
            .count();

        double disponibilidade = totalMaquinas > 0 ? Math.round(maquinasAtivas * 1000.0 / totalMaquinas) / 10.0 : 0;
        double mtbfDias = Math.round(computeMtbf(manutencoes) * 10) / 10.0;

        return new DashboardStatsDTO(
            totalMaquinas, manutencoes.size(), totalPecas,
            maquinasAtivas, maquinasInativas, maquinasEmManutencao,
            manutencoesPreventivas, manutencoesCorretivas, manutencoesVencidas,
            disponibilidade, mtbfDias,
            computeMonthlyCounts(manutencoes, hoje),
            computeAlerts(maquinas, hoje)
        );
    }

    private double computeMtbf(List<Manutencao> manutencoes) {
        Map<Long, List<LocalDate>> corretivas = manutencoes.stream()
            .filter(m -> "CORRETIVA".equals(m.getTipo()) && m.getDataManutencao() != null && m.getMaquina() != null)
            .collect(Collectors.groupingBy(
                m -> m.getMaquina().getId(),
                Collectors.mapping(Manutencao::getDataManutencao, Collectors.toList())
            ));

        List<Long> intervals = new ArrayList<>();
        corretivas.values().forEach(dates -> {
            dates.sort(Comparator.naturalOrder());
            for (int i = 1; i < dates.size(); i++) {
                intervals.add(ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i)));
            }
        });

        return intervals.isEmpty() ? 0.0 : intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private List<DashboardStatsDTO.MonthlyCount> computeMonthlyCounts(List<Manutencao> manutencoes, LocalDate hoje) {
        List<DashboardStatsDTO.MonthlyCount> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate ref = hoje.minusMonths(i).withDayOfMonth(1);
            int ano = ref.getYear();
            int mes = ref.getMonthValue();
            long count = manutencoes.stream()
                .filter(m -> m.getDataManutencao() != null
                    && m.getDataManutencao().getYear() == ano
                    && m.getDataManutencao().getMonthValue() == mes)
                .count();
            String label = MESES_PT[mes - 1] + "/" + String.valueOf(ano).substring(2);
            result.add(new DashboardStatsDTO.MonthlyCount(ano, mes, label, count));
        }
        return result;
    }

    private List<DashboardStatsDTO.OverdueAlert> computeAlerts(List<Maquina> maquinas, LocalDate hoje) {
        return maquinas.stream()
            .filter(m -> m.getIntervaloPreventivaDias() != null && m.getIntervaloPreventivaDias() > 0)
            .filter(m -> {
                if (m.getDataUltimaManutencao() == null) return true;
                return m.getDataUltimaManutencao().plusDays(m.getIntervaloPreventivaDias()).isBefore(hoje);
            })
            .map(m -> {
                long diasVencido = m.getDataUltimaManutencao() == null ? 999L
                    : ChronoUnit.DAYS.between(m.getDataUltimaManutencao().plusDays(m.getIntervaloPreventivaDias()), hoje);
                return new DashboardStatsDTO.OverdueAlert(
                    m.getId(), m.getNome(), m.getSetor(), diasVencido,
                    m.getPrioridade() != null ? m.getPrioridade() : "MEDIA"
                );
            })
            .sorted(Comparator.comparingLong(DashboardStatsDTO.OverdueAlert::diasVencido).reversed())
            .limit(10)
            .toList();
    }
}
