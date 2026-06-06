package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
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

/**
 * Maintenance CRUD, fully scoped to the caller's empresa. A maintenance can
 * only be attached to a machine that belongs to the same empresa, and every
 * read/delete is tenant-scoped.
 */
@Service
public class ManutencaoService {

    private static final Logger log = LoggerFactory.getLogger(ManutencaoService.class);

    private final ManutencaoRepository manutencaoRepository;
    private final MaquinaRepository maquinaRepository;
    private final TenantResolver tenant;

    public ManutencaoService(ManutencaoRepository manutencaoRepository,
                             MaquinaRepository maquinaRepository,
                             TenantResolver tenant) {
        this.manutencaoRepository = manutencaoRepository;
        this.maquinaRepository = maquinaRepository;
        this.tenant = tenant;
    }

    @Transactional
    public ManutencaoResponseDTO cadastrar(ManutencaoRequestDTO dto, Long maquinaId) {
        Long empresaId = tenant.requireEmpresaId();
        // The machine must belong to the caller's empresa — prevents attaching
        // a maintenance to another tenant's machine.
        Maquina maquina = maquinaRepository.findByIdAndEmpresaId(maquinaId, empresaId)
            .orElseThrow(() -> NotFoundException.of("Máquina", maquinaId));

        Manutencao m = new Manutencao();
        m.setEmpresaId(empresaId);
        m.setTipo(dto.tipo());
        m.setTecnico(dto.tecnico());
        m.setDescricao(dto.descricao());
        m.setPrioridade(dto.prioridade() != null ? dto.prioridade() : "MEDIA");
        m.setStatus(dto.status() != null ? dto.status() : "ABERTA");
        m.setDataManutencao(dto.dataManutencao() != null ? dto.dataManutencao() : LocalDate.now());
        m.setMaquina(maquina);

        log.info("Registrando manutenção tipo={} para máquina id={}", dto.tipo(), maquinaId);
        return toDTO(manutencaoRepository.save(m));
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
        return toDTO(manutencaoRepository.findByIdAndEmpresaId(id, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Manutenção", id)));
    }

    /** Soft delete — vide Manutencao.@SQLRestriction. */
    @Transactional
    public void deletar(Long id) {
        Manutencao m = manutencaoRepository.findByIdAndEmpresaId(id, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Manutenção", id));
        log.info("Soft-deleting manutenção id={}", id);
        m.setDeletedAt(LocalDateTime.now());
        manutencaoRepository.save(m);
    }

    public ManutencaoResponseDTO toDTO(Manutencao m) {
        ManutencaoResponseDTO.MaquinaInfo maquinaInfo = m.getMaquina() != null
            ? new ManutencaoResponseDTO.MaquinaInfo(m.getMaquina().getId(),
                                                   m.getMaquina().getNome(),
                                                   m.getMaquina().getSetor())
            : null;

        return new ManutencaoResponseDTO(
            m.getId(), m.getTipo(), m.getTecnico(), m.getDescricao(),
            m.getPrioridade() != null ? m.getPrioridade() : "MEDIA",
            m.getStatus()     != null ? m.getStatus()     : "ABERTA",
            m.getDataManutencao(),
            maquinaInfo
        );
    }
}
