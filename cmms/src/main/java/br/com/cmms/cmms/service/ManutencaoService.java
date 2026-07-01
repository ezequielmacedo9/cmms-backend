package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.AnexoDownloadDTO;
import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.ManutencaoAnexo;
import br.com.cmms.cmms.model.ManutencaoChecklistItem;
import br.com.cmms.cmms.model.ManutencaoPeca;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.ManutencaoAnexoRepository;
import br.com.cmms.cmms.repository.ManutencaoChecklistRepository;
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
    /** Max base64 length (~3 MB original file). */
    private static final int MAX_ANEXO_BASE64 = 4_000_000;

    private final ManutencaoRepository manutencaoRepository;
    private final MaquinaRepository maquinaRepository;
    private final PecaRepository pecaRepository;
    private final ManutencaoPecaRepository manutencaoPecaRepository;
    private final ManutencaoChecklistRepository checklistRepository;
    private final ManutencaoAnexoRepository anexoRepository;
    private final TenantResolver tenant;

    public ManutencaoService(ManutencaoRepository manutencaoRepository,
                             MaquinaRepository maquinaRepository,
                             PecaRepository pecaRepository,
                             ManutencaoPecaRepository manutencaoPecaRepository,
                             ManutencaoChecklistRepository checklistRepository,
                             ManutencaoAnexoRepository anexoRepository,
                             TenantResolver tenant) {
        this.manutencaoRepository = manutencaoRepository;
        this.maquinaRepository = maquinaRepository;
        this.pecaRepository = pecaRepository;
        this.manutencaoPecaRepository = manutencaoPecaRepository;
        this.checklistRepository = checklistRepository;
        this.anexoRepository = anexoRepository;
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

    // ── checklist ──────────────────────────────────────────────────────

    @Transactional
    public ManutencaoResponseDTO addChecklistItem(Long manutencaoId, String descricao) {
        Long empresaId = tenant.requireEmpresaId();
        Manutencao m = manutencaoRepository.findByIdAndEmpresaId(manutencaoId, empresaId)
            .orElseThrow(() -> NotFoundException.of("Manutenção", manutencaoId));
        checklistRepository.save(new ManutencaoChecklistItem(empresaId, manutencaoId, descricao));
        return toDetailDTO(m);
    }

    @Transactional
    public ManutencaoResponseDTO toggleChecklistItem(Long itemId) {
        Long empresaId = tenant.requireEmpresaId();
        ManutencaoChecklistItem item = checklistRepository.findByIdAndEmpresaId(itemId, empresaId)
            .orElseThrow(() -> NotFoundException.of("Item de checklist", itemId));
        item.setConcluido(!item.isConcluido());
        checklistRepository.save(item);
        return buscarPorId(item.getManutencaoId());
    }

    @Transactional
    public ManutencaoResponseDTO removeChecklistItem(Long itemId) {
        Long empresaId = tenant.requireEmpresaId();
        ManutencaoChecklistItem item = checklistRepository.findByIdAndEmpresaId(itemId, empresaId)
            .orElseThrow(() -> NotFoundException.of("Item de checklist", itemId));
        Long manutencaoId = item.getManutencaoId();
        checklistRepository.delete(item);
        return buscarPorId(manutencaoId);
    }

    // ── anexos ─────────────────────────────────────────────────────────

    @Transactional
    public ManutencaoResponseDTO addAnexo(Long manutencaoId, String nome, String contentType, String dadosBase64) {
        if (dadosBase64 == null || dadosBase64.isBlank()) {
            throw new ValidationException("ANEXO_VAZIO", "Arquivo do anexo é obrigatório.");
        }
        if (dadosBase64.length() > MAX_ANEXO_BASE64) {
            throw new ValidationException("ANEXO_MUITO_GRANDE", "Anexo muito grande (máx. ~3 MB).");
        }
        Long empresaId = tenant.requireEmpresaId();
        Manutencao m = manutencaoRepository.findByIdAndEmpresaId(manutencaoId, empresaId)
            .orElseThrow(() -> NotFoundException.of("Manutenção", manutencaoId));
        int tamanho = dadosBase64.length() * 3 / 4; // approximate decoded size
        anexoRepository.save(new ManutencaoAnexo(empresaId, manutencaoId,
            nome != null ? nome : "anexo", contentType, tamanho, dadosBase64));
        return toDetailDTO(m);
    }

    public AnexoDownloadDTO getAnexo(Long anexoId) {
        ManutencaoAnexo a = anexoRepository.findByIdAndEmpresaId(anexoId, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Anexo", anexoId));
        return new AnexoDownloadDTO(a.getId(), a.getNome(), a.getContentType(), a.getTamanho(), a.getDadosBase64());
    }

    @Transactional
    public ManutencaoResponseDTO removeAnexo(Long anexoId) {
        ManutencaoAnexo a = anexoRepository.findByIdAndEmpresaId(anexoId, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Anexo", anexoId));
        Long manutencaoId = a.getManutencaoId();
        anexoRepository.delete(a);
        return buscarPorId(manutencaoId);
    }

    // ── DTO mapping ────────────────────────────────────────────────────

    /** Lightweight DTO for list views — does not load parts/checklist/anexos (avoids N+1). */
    public ManutencaoResponseDTO toDTO(Manutencao m) {
        double maoObra = m.getCustoMaoObra() != null ? m.getCustoMaoObra() : 0.0;
        return new ManutencaoResponseDTO(
            m.getId(), m.getTipo(), m.getTecnico(), m.getTecnicoId(), m.getDescricao(),
            m.getPrioridade() != null ? m.getPrioridade() : "MEDIA",
            m.getStatus() != null ? m.getStatus() : "ABERTA",
            m.getDataManutencao(), m.getDataAbertura(), m.getDataConclusao(),
            m.getTempoExecucaoMinutos(), m.getCustoMaoObra(),
            0.0, maoObra, List.of(), List.of(), List.of(), maquinaInfo(m));
    }

    /** Full DTO with parts, checklist and attachment metadata — used for detail/create/status. */
    private ManutencaoResponseDTO toDetailDTO(Manutencao m) {
        List<ManutencaoPeca> pecas = manutencaoPecaRepository.findByManutencaoId(m.getId());
        double custoPecas = pecas.stream().mapToDouble(ManutencaoPeca::subtotal).sum();
        double maoObra = m.getCustoMaoObra() != null ? m.getCustoMaoObra() : 0.0;
        List<ManutencaoResponseDTO.PecaConsumida> pecasDto = pecas.stream()
            .map(p -> new ManutencaoResponseDTO.PecaConsumida(
                p.getPecaId(), p.getPecaNome(), p.getQuantidade(), p.getCustoUnitario(), p.subtotal()))
            .toList();
        List<ManutencaoResponseDTO.ChecklistItem> checklistDto =
            checklistRepository.findByManutencaoIdOrderById(m.getId()).stream()
                .map(c -> new ManutencaoResponseDTO.ChecklistItem(c.getId(), c.getDescricao(), c.isConcluido()))
                .toList();
        List<ManutencaoResponseDTO.Anexo> anexosDto =
            anexoRepository.findMetaByManutencaoId(m.getId()).stream()
                .map(r -> new ManutencaoResponseDTO.Anexo(
                    ((Number) r[0]).longValue(), (String) r[1], (String) r[2], ((Number) r[3]).intValue()))
                .toList();
        return new ManutencaoResponseDTO(
            m.getId(), m.getTipo(), m.getTecnico(), m.getTecnicoId(), m.getDescricao(),
            m.getPrioridade() != null ? m.getPrioridade() : "MEDIA",
            m.getStatus() != null ? m.getStatus() : "ABERTA",
            m.getDataManutencao(), m.getDataAbertura(), m.getDataConclusao(),
            m.getTempoExecucaoMinutos(), m.getCustoMaoObra(),
            custoPecas, maoObra + custoPecas, pecasDto, checklistDto, anexosDto, maquinaInfo(m));
    }

    private ManutencaoResponseDTO.MaquinaInfo maquinaInfo(Manutencao m) {
        return m.getMaquina() != null
            ? new ManutencaoResponseDTO.MaquinaInfo(m.getMaquina().getId(), m.getMaquina().getNome(), m.getMaquina().getSetor())
            : null;
    }
}
