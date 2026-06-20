package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.DashboardStatsDTO;
import br.com.cmms.cmms.dto.RelatorioGerencialDTO;
import br.com.cmms.cmms.model.Peca;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioGerencialServiceTest {

    @Mock DashboardService dashboardService;
    @Mock MaquinaRepository maquinaRepository;
    @Mock ManutencaoRepository manutencaoRepository;
    @Mock PecaRepository pecaRepository;
    @Mock TenantResolver tenant;
    @InjectMocks RelatorioGerencialService service;

    @BeforeEach
    void setUp() {
        lenient().when(tenant.requireEmpresaId()).thenReturn(1L);
    }

    private DashboardStatsDTO stats() {
        return new DashboardStatsDTO(
            10, 20, 7,        // totalMaquinas, totalManutencoes, totalPecas
            8, 1, 1,          // ativas, inativas, em manutencao
            12, 8, 2,         // preventivas, corretivas, vencidas
            80.0, 15.0,       // disponibilidade, mtbf
            List.of(), List.of());
    }

    @Test
    @DisplayName("gerar: calcula cumprimento de preventiva, valor de estoque e top ofensores")
    void gerar_computesKpis() {
        when(dashboardService.getStats()).thenReturn(stats());
        // 5 máquinas com intervalo de preventiva; 2 vencidas -> 60% de cumprimento.
        when(maquinaRepository.findPreventiveCandidates(1L)).thenReturn(List.of(
            new Object[]{}, new Object[]{}, new Object[]{}, new Object[]{}, new Object[]{}));
        when(pecaRepository.findByEmpresaId(1L)).thenReturn(List.of(
            peca(10.0, 3),   // 30
            peca(5.0, 4)));  // 20  -> total 50
        when(manutencaoRepository.findTopOfensores(1L)).thenReturn(List.of(
            new Object[]{ 1L, "Torno", 5L },
            new Object[]{ 2L, "Prensa", 3L }));

        RelatorioGerencialDTO out = service.gerar();

        assertThat(out.totalMaquinas()).isEqualTo(10);
        assertThat(out.manutencoesPreventivas()).isEqualTo(12);
        assertThat(out.manutencoesCorretivas()).isEqualTo(8);
        assertThat(out.maquinasComPreventiva()).isEqualTo(5);
        assertThat(out.preventivasVencidas()).isEqualTo(2);
        assertThat(out.cumprimentoPreventivaPct()).isEqualTo(60.0);
        assertThat(out.disponibilidade()).isEqualTo(80.0);
        assertThat(out.mtbfDias()).isEqualTo(15.0);
        assertThat(out.valorTotalEstoque()).isEqualTo(50.0);
        assertThat(out.topOfensores()).hasSize(2);
        assertThat(out.topOfensores().get(0).maquinaNome()).isEqualTo("Torno");
        assertThat(out.topOfensores().get(0).corretivas()).isEqualTo(5);
    }

    @Test
    @DisplayName("gerar: sem máquinas com preventiva, cumprimento é 100%")
    void gerar_noPreventive_is100() {
        when(dashboardService.getStats()).thenReturn(stats());
        when(maquinaRepository.findPreventiveCandidates(1L)).thenReturn(List.of());
        when(pecaRepository.findByEmpresaId(1L)).thenReturn(List.of());
        when(manutencaoRepository.findTopOfensores(1L)).thenReturn(List.of());

        RelatorioGerencialDTO out = service.gerar();

        assertThat(out.cumprimentoPreventivaPct()).isEqualTo(100.0);
        assertThat(out.valorTotalEstoque()).isZero();
        assertThat(out.topOfensores()).isEmpty();
    }

    private static Peca peca(double custo, int qtd) {
        Peca p = new Peca();
        p.setCustoUnitario(custo);
        p.setQuantidadeEmEstoque(qtd);
        return p;
    }
}
