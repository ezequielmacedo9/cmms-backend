package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.DashboardStatsDTO;
import br.com.cmms.cmms.dto.RelatorioGerencialDTO;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PecaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Computes the management report KPIs for the caller's empresa. Reuses
 * {@link DashboardService} for the figures it already aggregates (totals,
 * MTBF, availability, overdue count) and adds preventive-compliance, stock
 * value and the corrective "top offenders" ranking on top.
 */
@Service
public class RelatorioGerencialService {

    private static final int TOP_OFENSORES = 5;

    private final DashboardService dashboardService;
    private final MaquinaRepository maquinaRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final PecaRepository pecaRepository;
    private final TenantResolver tenant;

    public RelatorioGerencialService(DashboardService dashboardService,
                                     MaquinaRepository maquinaRepository,
                                     ManutencaoRepository manutencaoRepository,
                                     PecaRepository pecaRepository,
                                     TenantResolver tenant) {
        this.dashboardService = dashboardService;
        this.maquinaRepository = maquinaRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.pecaRepository = pecaRepository;
        this.tenant = tenant;
    }

    @Transactional(readOnly = true)
    public RelatorioGerencialDTO gerar() {
        Long empresaId = tenant.requireEmpresaId();
        DashboardStatsDTO stats = dashboardService.getStats();

        long maquinasComPreventiva = maquinaRepository.findPreventiveCandidates(empresaId).size();
        long vencidas = stats.manutencoesVencidas();
        double cumprimentoPct = maquinasComPreventiva > 0
            ? Math.round((maquinasComPreventiva - vencidas) * 1000.0 / maquinasComPreventiva) / 10.0
            : 100.0;

        double valorEstoque = pecaRepository.findByEmpresaId(empresaId).stream()
            .mapToDouble(p -> p.getCustoUnitario() * p.getQuantidadeEmEstoque())
            .sum();
        valorEstoque = Math.round(valorEstoque * 100.0) / 100.0;

        List<RelatorioGerencialDTO.Ofensor> ofensores = manutencaoRepository.findTopOfensores(empresaId).stream()
            .limit(TOP_OFENSORES)
            .map(row -> new RelatorioGerencialDTO.Ofensor(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).longValue()))
            .toList();

        return new RelatorioGerencialDTO(
            stats.totalMaquinas(),
            stats.totalManutencoes(),
            stats.manutencoesPreventivas(),
            stats.manutencoesCorretivas(),
            maquinasComPreventiva,
            vencidas,
            cumprimentoPct,
            stats.disponibilidade(),
            stats.mtbfDias(),
            valorEstoque,
            ofensores
        );
    }
}
