package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.DashboardStatsDTO;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PecaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardService}. Mocks the three repositories so
 * the tests don't depend on the database — the focus is on the assembly /
 * arithmetic logic in the service.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock MaquinaRepository maquinaRepository;
    @Mock ManutencaoRepository manutencaoRepository;
    @Mock PecaRepository pecaRepository;
    @Mock TenantResolver tenant;

    @InjectMocks DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        // Sensible defaults — individual tests override when needed.
        lenient().when(tenant.requireEmpresaId()).thenReturn(1L);
        lenient().when(maquinaRepository.countGroupByStatus(anyLong())).thenReturn(List.of());
        lenient().when(manutencaoRepository.countGroupByTipo(anyLong())).thenReturn(List.of());
        lenient().when(maquinaRepository.findPreventiveCandidates(anyLong())).thenReturn(List.of());
        lenient().when(manutencaoRepository.findCorrectiveDatesPerMachine(anyLong())).thenReturn(List.of());
        lenient().when(manutencaoRepository.monthlyCountsSince(any(), anyLong())).thenReturn(List.of());
        lenient().when(pecaRepository.countByEmpresaId(anyLong())).thenReturn(0L);
    }

    @Test
    @DisplayName("getStats: base zerada devolve KPIs todos em zero (sem NPE)")
    void getStats_emptyDataset() {
        DashboardStatsDTO out = dashboardService.getStats();

        assertThat(out.totalMaquinas()).isZero();
        assertThat(out.totalManutencoes()).isZero();
        assertThat(out.totalPecas()).isZero();
        assertThat(out.disponibilidade()).isZero();
        assertThat(out.mtbfDias()).isZero();
        assertThat(out.ultimosSeisMeses()).hasSize(6);
        assertThat(out.alertasVencidos()).isEmpty();
    }

    @Test
    @DisplayName("getStats: agrega corretamente status e tipos vindos dos repositórios")
    void getStats_aggregatesStatusAndTipo() {
        when(maquinaRepository.countGroupByStatus(1L)).thenReturn(List.of(
            row("ATIVO", 8L),
            row("INATIVO", 1L),
            row("EM_MANUTENCAO", 1L)
        ));
        when(manutencaoRepository.countGroupByTipo(1L)).thenReturn(List.of(
            row("PREVENTIVA", 12L),
            row("CORRETIVA", 3L)
        ));
        when(pecaRepository.countByEmpresaId(1L)).thenReturn(42L);

        DashboardStatsDTO out = dashboardService.getStats();

        assertThat(out.totalMaquinas()).isEqualTo(10);
        assertThat(out.maquinasAtivas()).isEqualTo(8);
        assertThat(out.maquinasInativas()).isEqualTo(1);
        assertThat(out.maquinasEmManutencao()).isEqualTo(1);

        assertThat(out.totalManutencoes()).isEqualTo(15);
        assertThat(out.manutencoesPreventivas()).isEqualTo(12);
        assertThat(out.manutencoesCorretivas()).isEqualTo(3);

        assertThat(out.totalPecas()).isEqualTo(42);
        // 80% disponibilidade = 8 / 10
        assertThat(out.disponibilidade()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("MTBF: média de intervalos entre corretivas da mesma máquina")
    void mtbf_averageOfIntervalsPerMachine() {
        // Machine 1: 3 corretivas — intervalos de 10 e 20 dias  → média 15
        // Machine 2: 2 corretivas — intervalo de 30 dias       → 30
        // Esperado: média de [10, 20, 30] = 20 dias
        when(manutencaoRepository.findCorrectiveDatesPerMachine(1L)).thenReturn(List.of(
            new Object[]{ 1L, LocalDate.of(2026, 1, 1) },
            new Object[]{ 1L, LocalDate.of(2026, 1, 11) },
            new Object[]{ 1L, LocalDate.of(2026, 1, 31) },
            new Object[]{ 2L, LocalDate.of(2026, 2, 1) },
            new Object[]{ 2L, LocalDate.of(2026, 3, 3) }
        ));

        DashboardStatsDTO out = dashboardService.getStats();

        assertThat(out.mtbfDias()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("alertas: ordena por dias vencidos descendentes e limita ao top 10")
    void alerts_orderedByOverdueDescAndLimited() {
        LocalDate hoje = LocalDate.now();
        // 12 máquinas: as 10 mais atrasadas devem aparecer no topo.
        List<Object[]> rows = new java.util.ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            // intervalo = 10 dias; dataUltimaManutencao = hoje - (10 + i) dias
            // → atraso = i dias
            rows.add(new Object[]{
                (long) i, "Maquina " + i, "Setor", "MEDIA",
                hoje.minusDays(10 + i), 10
            });
        }
        when(maquinaRepository.findPreventiveCandidates(1L)).thenReturn(rows);

        DashboardStatsDTO out = dashboardService.getStats();

        assertThat(out.manutencoesVencidas()).isEqualTo(12);
        assertThat(out.alertasVencidos()).hasSize(10);
        assertThat(out.alertasVencidos().get(0).diasVencido())
            .isGreaterThan(out.alertasVencidos().get(9).diasVencido());
    }

    @Test
    @DisplayName("monthlyCounts: sempre devolve 6 buckets, preenchendo zeros nos meses sem dados")
    void monthlyCounts_alwaysSixBuckets() {
        DashboardStatsDTO out = dashboardService.getStats();
        assertThat(out.ultimosSeisMeses()).hasSize(6);
        // Quando todos os meses estão vazios, todos devem ter total = 0
        assertThat(out.ultimosSeisMeses()).allMatch(m -> m.total() == 0L);
    }

    private static Object[] row(String key, long count) {
        return new Object[]{ key, count };
    }
}
