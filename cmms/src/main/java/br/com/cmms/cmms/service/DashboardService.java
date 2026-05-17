package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.DashboardStatsDTO;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PecaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds the {@link DashboardStatsDTO} consumed by the home dashboard.
 *
 * <p>Pre-optimisation history: this service used to call {@code findAll()}
 * on machines + maintenances and iterate everything in memory — which broke
 * spectacularly as soon as the database held real data. The current
 * implementation pushes the work to the database with GROUP BY aggregations
 * and only loads scalar rows.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final String[] MESES_PT = {"Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"};
    private static final int MONTH_WINDOW = 6;
    private static final int MAX_ALERTS = 10;

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
    @Transactional(readOnly = true)
    public DashboardStatsDTO getStats() {
        log.debug("Computing dashboard stats");

        LocalDate hoje = LocalDate.now();
        LocalDate sixMonthsAgo = hoje.minusMonths(MONTH_WINDOW - 1L).withDayOfMonth(1);

        // ── Machine status counts (single GROUP BY query) ──
        Map<String, Long> statusCounts = toCountMap(maquinaRepository.countGroupByStatus());
        long maquinasAtivas        = statusCounts.getOrDefault("ATIVO", 0L);
        long maquinasInativas      = statusCounts.getOrDefault("INATIVO", 0L);
        long maquinasEmManutencao  = statusCounts.getOrDefault("EM_MANUTENCAO", 0L);
        long totalMaquinas         = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        // ── Maintenance type counts (single GROUP BY query) ──
        Map<String, Long> tipoCounts = toCountMap(manutencaoRepository.countGroupByTipo());
        long manutencoesPreventivas = tipoCounts.getOrDefault("PREVENTIVA", 0L);
        long manutencoesCorretivas  = tipoCounts.getOrDefault("CORRETIVA",  0L);
        long totalManutencoes       = tipoCounts.values().stream().mapToLong(Long::longValue).sum();

        long totalPecas = pecaRepository.count();

        // ── Overdue preventives (database returns only candidates with interval > 0) ──
        List<PreventiveRow> preventiveCandidates = maquinaRepository.findPreventiveCandidates().stream()
            .map(PreventiveRow::from)
            .toList();
        long manutencoesVencidas = preventiveCandidates.stream()
            .filter(p -> p.isOverdue(hoje))
            .count();

        // ── Headline KPIs ──
        double disponibilidade = totalMaquinas > 0
            ? Math.round(maquinasAtivas * 1000.0 / totalMaquinas) / 10.0
            : 0;
        double mtbfDias = Math.round(computeMtbf() * 10) / 10.0;

        return new DashboardStatsDTO(
            totalMaquinas, totalManutencoes, totalPecas,
            maquinasAtivas, maquinasInativas, maquinasEmManutencao,
            manutencoesPreventivas, manutencoesCorretivas, manutencoesVencidas,
            disponibilidade, mtbfDias,
            computeMonthlyCounts(hoje, sixMonthsAgo),
            computeAlerts(preventiveCandidates, hoje)
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> out = new HashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "";
            long value = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            out.merge(key, value, Long::sum);
        }
        return out;
    }

    private double computeMtbf() {
        List<Object[]> rows = manutencaoRepository.findCorrectiveDatesPerMachine();
        if (rows.isEmpty()) return 0.0;

        // The query already orders rows by (machine_id, date), so we can scan
        // linearly and compute gaps without grouping into a Map.
        List<Long> intervals = new ArrayList<>();
        Long previousMachineId = null;
        LocalDate previousDate = null;
        for (Object[] row : rows) {
            Long machineId = ((Number) row[0]).longValue();
            LocalDate date = (LocalDate) row[1];
            if (machineId.equals(previousMachineId) && previousDate != null) {
                intervals.add(ChronoUnit.DAYS.between(previousDate, date));
            }
            previousMachineId = machineId;
            previousDate = date;
        }
        return intervals.isEmpty() ? 0.0
            : intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private List<DashboardStatsDTO.MonthlyCount> computeMonthlyCounts(LocalDate hoje, LocalDate start) {
        // Map from (year * 100 + month) → count, populated from a single GROUP BY query.
        TreeMap<Integer, Long> aggregated = new TreeMap<>();
        for (Object[] row : manutencaoRepository.monthlyCountsSince(start)) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            aggregated.put(year * 100 + month, count);
        }

        List<DashboardStatsDTO.MonthlyCount> result = new ArrayList<>(MONTH_WINDOW);
        for (int i = MONTH_WINDOW - 1; i >= 0; i--) {
            LocalDate ref = hoje.minusMonths(i).withDayOfMonth(1);
            int year  = ref.getYear();
            int month = ref.getMonthValue();
            long count = aggregated.getOrDefault(year * 100 + month, 0L);
            String label = MESES_PT[month - 1] + "/" + String.valueOf(year).substring(2);
            result.add(new DashboardStatsDTO.MonthlyCount(year, month, label, count));
        }
        return result;
    }

    private List<DashboardStatsDTO.OverdueAlert> computeAlerts(List<PreventiveRow> rows, LocalDate hoje) {
        return rows.stream()
            .filter(p -> p.isOverdue(hoje))
            .map(p -> new DashboardStatsDTO.OverdueAlert(
                p.id, p.nome, p.setor, p.daysOverdue(hoje),
                p.prioridade != null ? p.prioridade : "MEDIA"))
            .sorted(Comparator.comparingLong(DashboardStatsDTO.OverdueAlert::diasVencido).reversed())
            .limit(MAX_ALERTS)
            .toList();
    }

    /**
     * Compact projection of the columns we read from the preventive query.
     * Keeps the rest of the code working with named fields instead of
     * {@code Object[]} indices.
     */
    private record PreventiveRow(
        Long id, String nome, String setor, String prioridade,
        LocalDate dataUltimaManutencao, Integer intervaloPreventivaDias
    ) {
        static PreventiveRow from(Object[] row) {
            return new PreventiveRow(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (LocalDate) row[4],
                row[5] != null ? ((Number) row[5]).intValue() : null
            );
        }
        boolean isOverdue(LocalDate today) {
            if (intervaloPreventivaDias == null || intervaloPreventivaDias <= 0) return false;
            if (dataUltimaManutencao == null) return true;
            return dataUltimaManutencao.plusDays(intervaloPreventivaDias).isBefore(today);
        }
        long daysOverdue(LocalDate today) {
            if (dataUltimaManutencao == null) return 999L;
            return ChronoUnit.DAYS.between(dataUltimaManutencao.plusDays(intervaloPreventivaDias), today);
        }
    }
}
