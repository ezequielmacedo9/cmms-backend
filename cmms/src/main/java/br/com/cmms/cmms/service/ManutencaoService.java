package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.ManutencaoPeca;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.ManutencaoPecaRepository;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PecaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Work-order (manutenção) use cases, fully scoped to the caller's empresa.
 * Beyond CRUD it handles the work-order workflow: consuming parts (which
 * decrements stock with a cost snapshot), labor time/cost, status transitions
 * and the open/close dates that feed MTTR.
 */
@Service
public class ManutencaoService {

    private static final Logger log = LoggerFactory.getLogger(ManutencaoService.class);
    private static final Set<String> STATUS_VALIDOS =
        Set.of("ABERTA", "EM_ANDAMENTO", "CONCLUIDA", "CANCELADA");

    private final ManutencaoRepository manutencaoRepository;
    private final MaquinaRepository maquinaRepository;
    private final PecaRepository pecaRepository;
    private final ManutencaoPecaRepository manutencaoPecaRepository;
    private final TenantResolver tenant;

    public ManutencaoService(ManutencaoRepository manutencaoRepository,
                             MaquinaRepository maquinaRepository,
                             PecaRepository pecaRepository,
                             ManutencaoPecaRepository manutencaoPecaRepository,
                             TenantResolver tenant) {
        this.manutencaoRepository = manutencaoRepository;
        this.maquinaRepository = maquinaRepository;
        this.pecaRepository = pecaRepository;
        this.manutencaoPecaRepository = manutencaoPecaRepository;
        this.tenant = tenant;
    }

    @Transactional
    public ManutencaoResponseDTO cadastrar(ManutencaoRequestDTO dto, Long maquinaId) {
        Long empresaId = tenant.requireEmpresaId();
        Maquina maquina = maquinaRepository.findByIdAndEmpresaId(maquinaId, empresaId)
            .orElseThrow(() -> NotFoundException.of("Máquina", maquinaId));

        Manutencao m = new Manutencao();
        m.setEmpresaId(empresaId);
        m.setTipo(dto.tipo());
        m.setTecnico(dto.tecnico());
        m.setTecnicoId(dto.tecnicoId());
        m.setDescricao(dto.descricao());
        m.setPrioridade(dto.prioridade() != null ? dto.prioridade() : "MEDIA");
        m.setStatus(dto.status() != null ? dto.status() : "ABERTA");
        m.setDataManutencao(dto.dataManutencao() != null ? dto.dataManutencao() : LocalDate.now());
        m.setDataAbertura(LocalDate.now());
        m.setTempoExecucaoMinutos(dto.tempoExecucaoMinutos());
        m.setCustoMaoObra(dto.custoMaoObra());
        m.setMaquina(maquina);
        if ("CONCLUIDA".equals(m.getStatus())) m.setDataConclusao(LocalDate.now());

        Manutencao saved = manutencaoRepository.save(m);
        consumirPecas(saved, dto.pecas(), empresaId);

        log.info("Registrando OS tipo={} para máquina id={}", dto.tipo(), maquinaId);
        return toDetailDTO(saved);
    }

    /** Moves the work order through its status workflow, stamping the close date on completion. */
    @Transactional
    public ManutencaoResponseDTO alterarStatus(Long id, String novoStatus) {
        if (novoStatus == null || !STATUS_VALIDOS.contains(novoStatus)) {
            throw new ValidationException("INVALID_STATUS", "Status inválido: " + novoStatus);
        }
        Manutencao m = manutencaoRepository.findByIdAndEmpresaId(id, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Manutenção", id));
        m.setStatus(novoStatus);
        if ("CONCLUIDA".equals(novoStatus) && m.getDataConclusao() == null) {
            m.setDataConclusao(LocalDate.now());
        }
        log.info("OS id={} -> status {}", id, novoStatus);
        return toDetailDTO(manutencaoRepository.save(m));
    }

    /** Legacy non-paged listing. Kept for reports and integrations. */
    public List<ManutencaoResponseDTO> listar() {
        return manutencaoRepository.findByEmpresaIdOrderByDataManutencaoDesc(tenant.requireEmpresaId())
            .stream().map(this::toDTO).toList();
    }

    /** Preferred listing endpoint — paged, eager-fetches the machine to avoid N+1. */
    public Page<ManutencaoResponseDTO> listar(Pageable pageable) {
        return manutencaoRepository.findByEmpresaId(tenant.requireEmpresaId(), pageable).map(this::toDTO);
    }

    public List<ManutencaoResponseDTO> listarPorMaquina(Long maquinaId) {
        return manutencaoRepository
            .findByMaquinaIdAndEmpresaIdOrderByDataManutencaoDesc(maquinaId, tenant.requireEmpresaId())
            .stream().map(this::toDTO).toList();
    }

    public Page<ManutencaoResponseDTO> listarPorMaquina(Long maquinaId, Pageable pageable) {
        return manutencaoRepository
            .findByMaquinaIdAndEmpresaIdOrderByDataManutencaoDesc(maquinaId, tenant.requireEmpresaId(), pageable)
            .map(this::toDTO);
    }

    public ManutencaoResponseDTO buscarPorId(Long id) {
        return toDetailDTO(manutencaoRepository.findByIdAndEmpresaId(id, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Manutenção", id)));
    }

    /** Soft delete — vide Manutencao.@SQLRestriction. Stock already consumed is NOT restored. */
    @Transactional
    public void deletar(Long id) {
        Manutencao m = manutencaoRepository.findByIdAndEmpresaId(id, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Manutenção", id));
        log.info("Soft-deleting manutenção id={}", id);
        m.setDeletedAt(LocalDateTime.now());
        manutencaoRepository.save(m);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** Validates each part belongs to the empresa, has stock, then decrements it and records consumption. */
    private void consumirPecas(Manutencao m, List<ManutencaoRequestDTO.PecaConsumo> pecas, Long empresaId) {
        if (pecas == null || pecas.isEmpty()) return;
        for (ManutencaoRequestDTO.PecaConsumo pc : pecas) {
            if (pc.pecaId() == null || pc.quantidade() == null || pc.quantidade() <= 0) continue;
            Peca peca = pecaRepository.findByIdAndEmpresaId(pc.pecaId(), empresaId)
                .orElseThrow(() -> NotFoundException.of("Peça", pc.pecaId()));
            if (peca.getQuantidadeEmEstoque() < pc.quantidade()) {
                throw new ValidationException("ESTOQUE_INSUFICIENTE",
                    "Estoque insuficiente para a peça " + peca.getNome()
                        + " (disponível: " + peca.getQuantidadeEmEstoque() + ").");
            }
            peca.setQuantidadeEmEstoque(peca.getQuantidadeEmEstoque() - pc.quantidade());
            pecaRepository.save(peca);
            manutencaoPecaRepository.save(new ManutencaoPeca(
                empresaId, m.getId(), peca.getId(), peca.getNome(), pc.quantidade(), peca.getCustoUnitario()));
        }
    }

    /** Lightweight DTO for list views — does not load consumed parts (avoids N+1). */
    public ManutencaoResponseDTO toDTO(Manutencao m) {
        double maoObra = m.getCustoMaoObra() != null ? m.getCustoMaoObra() : 0.0;
        return new ManutencaoResponseDTO(
            m.getId(), m.getTipo(), m.getTecnico(), m.getTecnicoId(), m.getDescricao(),
            m.getPrioridade() != null ? m.getPrioridade() : "MEDIA",
            m.getStatus() != null ? m.getStatus() : "ABERTA",
            m.getDataManutencao(), m.getDataAbertura(), m.getDataConclusao(),
            m.getTempoExecucaoMinutos(), m.getCustoMaoObra(),
            0.0, maoObra, List.of(), maquinaInfo(m));
    }

    /** Full DTO with consumed parts + computed costs — used for detail/create/status. */
    private ManutencaoResponseDTO toDetailDTO(Manutencao m) {
        List<ManutencaoPeca> pecas = manutencaoPecaRepository.findByManutencaoId(m.getId());
        double custoPecas = pecas.stream().mapToDouble(ManutencaoPeca::subtotal).sum();
        double maoObra = m.getCustoMaoObra() != null ? m.getCustoMaoObra() : 0.0;
        List<ManutencaoResponseDTO.PecaConsumida> pecasDto = pecas.stream()
            .map(p -> new ManutencaoResponseDTO.PecaConsumida(
                p.getPecaId(), p.getPecaNome(), p.getQuantidade(), p.getCustoUnitario(), p.subtotal()))
            .toList();
        return new ManutencaoResponseDTO(
            m.getId(), m.getTipo(), m.getTecnico(), m.getTecnicoId(), m.getDescricao(),
            m.getPrioridade() != null ? m.getPrioridade() : "MEDIA",
            m.getStatus() != null ? m.getStatus() : "ABERTA",
            m.getDataManutencao(), m.getDataAbertura(), m.getDataConclusao(),
            m.getTempoExecucaoMinutos(), m.getCustoMaoObra(),
            custoPecas, maoObra + custoPecas, pecasDto, maquinaInfo(m));
    }

    private ManutencaoResponseDTO.MaquinaInfo maquinaInfo(Manutencao m) {
        return m.getMaquina() != null
            ? new ManutencaoResponseDTO.MaquinaInfo(m.getMaquina().getId(), m.getMaquina().getNome(), m.getMaquina().getSetor())
            : null;
    }
}
