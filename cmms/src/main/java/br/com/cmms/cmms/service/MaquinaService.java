package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.MaquinaRequestDTO;
import br.com.cmms.cmms.dto.MaquinaResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MaquinaService {

    private static final Logger log = LoggerFactory.getLogger(MaquinaService.class);

    private final MaquinaRepository maquinaRepository;

    public MaquinaService(MaquinaRepository maquinaRepository) {
        this.maquinaRepository = maquinaRepository;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "maquinas",        allEntries = true),
        @CacheEvict(value = "maquinas-page",   allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public MaquinaResponseDTO cadastrar(MaquinaRequestDTO dto) {
        Maquina m = new Maquina();
        applyDto(dto, m);
        log.info("Cadastrando máquina: {}", dto.nome());
        return toDTO(maquinaRepository.save(m));
    }

    /**
     * Paged listing with optional search and status filter. Cached on the
     * exact tuple (query, status, page, size, sort).
     */
    @Cacheable(value = "maquinas-page",
        key = "(#q ?: '') + '|' + (#status ?: '') + '|' + #pageable.pageNumber + '|' + #pageable.pageSize + '|' + #pageable.sort")
    public Page<MaquinaResponseDTO> listar(String q, String status, Pageable pageable) {
        String normalizedQ      = (q == null      || q.isBlank())      ? null : q.trim();
        String normalizedStatus = (status == null || status.isBlank()) ? null : status.trim();
        return maquinaRepository.search(normalizedQ, normalizedStatus, pageable).map(this::toDTO);
    }

    /** Legacy non-paged listing — used by integrations that still expect a full list. */
    @Cacheable("maquinas")
    public List<MaquinaResponseDTO> listar() {
        return maquinaRepository.findAll().stream().map(this::toDTO).toList();
    }

    public MaquinaResponseDTO buscarPorId(Long id) {
        return toDTO(maquinaRepository.findById(id)
            .orElseThrow(() -> NotFoundException.of("Máquina", id)));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "maquinas",        allEntries = true),
        @CacheEvict(value = "maquinas-page",   allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public MaquinaResponseDTO atualizar(Long id, MaquinaRequestDTO dto) {
        Maquina m = maquinaRepository.findById(id)
            .orElseThrow(() -> NotFoundException.of("Máquina", id));
        applyDto(dto, m);
        log.info("Atualizando máquina id={}", id);
        return toDTO(maquinaRepository.save(m));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "maquinas",        allEntries = true),
        @CacheEvict(value = "maquinas-page",   allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public void deletar(Long id) {
        if (!maquinaRepository.existsById(id)) {
            throw NotFoundException.of("Máquina", id);
        }
        log.info("Deletando máquina id={}", id);
        maquinaRepository.deleteById(id);
    }

    private void applyDto(MaquinaRequestDTO dto, Maquina m) {
        m.setNome(dto.nome());
        m.setSetor(dto.setor());
        m.setStatus(dto.status());
        m.setPrioridade(dto.prioridade() != null ? dto.prioridade()
                                                 : (m.getPrioridade() != null ? m.getPrioridade() : "MEDIA"));
        m.setIntervaloPreventivaDias(dto.intervaloPreventivaDias() != null ? dto.intervaloPreventivaDias() : 0);
        m.setDataUltimaManutencao(dto.dataUltimaManutencao());
    }

    public MaquinaResponseDTO toDTO(Maquina m) {
        return new MaquinaResponseDTO(
            m.getId(), m.getNome(), m.getSetor(), m.getStatus(),
            m.getPrioridade() != null ? m.getPrioridade() : "MEDIA",
            m.getIntervaloPreventivaDias(),
            m.getDataUltimaManutencao(),
            isVencida(m)
        );
    }

    private boolean isVencida(Maquina m) {
        if (m.getIntervaloPreventivaDias() == null || m.getIntervaloPreventivaDias() == 0) return false;
        if (m.getDataUltimaManutencao() == null) return true;
        return m.getDataUltimaManutencao().plusDays(m.getIntervaloPreventivaDias()).isBefore(LocalDate.now());
    }
}
