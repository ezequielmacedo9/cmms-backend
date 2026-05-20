package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ManutencaoService {

    private static final Logger log = LoggerFactory.getLogger(ManutencaoService.class);

    private final ManutencaoRepository manutencaoRepository;
    private final MaquinaRepository maquinaRepository;

    public ManutencaoService(ManutencaoRepository manutencaoRepository,
                             MaquinaRepository maquinaRepository) {
        this.manutencaoRepository = manutencaoRepository;
        this.maquinaRepository = maquinaRepository;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manutencoes",      allEntries = true),
        @CacheEvict(value = "manutencoes-page", allEntries = true),
        @CacheEvict(value = "dashboard-stats",  allEntries = true)
    })
    public ManutencaoResponseDTO cadastrar(ManutencaoRequestDTO dto, Long maquinaId) {
        Maquina maquina = maquinaRepository.findById(maquinaId)
            .orElseThrow(() -> NotFoundException.of("Máquina", maquinaId));

        Manutencao m = new Manutencao();
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
        return manutencaoRepository.findAll().stream().map(this::toDTO).toList();
    }

    /** Preferred listing endpoint — paged, eager-fetches the machine to avoid N+1. */
    public Page<ManutencaoResponseDTO> listar(Pageable pageable) {
        return manutencaoRepository.findAll(pageable).map(this::toDTO);
    }

    public List<ManutencaoResponseDTO> listarPorMaquina(Long maquinaId) {
        return manutencaoRepository.findByMaquinaIdOrderByDataManutencaoDesc(maquinaId)
            .stream().map(this::toDTO).toList();
    }

    public Page<ManutencaoResponseDTO> listarPorMaquina(Long maquinaId, Pageable pageable) {
        return manutencaoRepository.findByMaquinaIdOrderByDataManutencaoDesc(maquinaId, pageable)
            .map(this::toDTO);
    }

    public ManutencaoResponseDTO buscarPorId(Long id) {
        return toDTO(manutencaoRepository.findById(id)
            .orElseThrow(() -> NotFoundException.of("Manutenção", id)));
    }

    /** Soft delete — vide Manutencao.@SQLRestriction. */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manutencoes",      allEntries = true),
        @CacheEvict(value = "manutencoes-page", allEntries = true),
        @CacheEvict(value = "dashboard-stats",  allEntries = true)
    })
    public void deletar(Long id) {
        Manutencao m = manutencaoRepository.findById(id)
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
