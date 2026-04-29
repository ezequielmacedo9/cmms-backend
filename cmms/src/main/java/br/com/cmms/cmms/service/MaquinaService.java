package br.com.cmms.cmms.service;

import br.com.cmms.cmms.Security.TenantContext;
import br.com.cmms.cmms.dto.MaquinaRequestDTO;
import br.com.cmms.cmms.dto.MaquinaResponseDTO;
import br.com.cmms.cmms.model.Empresa;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.EmpresaRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class MaquinaService {

    private static final Logger log = LoggerFactory.getLogger(MaquinaService.class);

    private final MaquinaRepository maquinaRepository;
    private final EmpresaRepository empresaRepository;

    public MaquinaService(MaquinaRepository maquinaRepository, EmpresaRepository empresaRepository) {
        this.maquinaRepository = maquinaRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "maquinas", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public MaquinaResponseDTO cadastrar(MaquinaRequestDTO dto) {
        Maquina m = new Maquina();
        applyDto(dto, m);
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            m.setEmpresa(empresaRepository.getReferenceById(empresaId));
        }
        log.info("Cadastrando máquina: {} (empresa={})", dto.nome(), empresaId);
        return toDTO(maquinaRepository.save(m));
    }

    @Cacheable(value = "maquinas", key = "T(br.com.cmms.cmms.Security.TenantContext).getEmpresaId()")
    public List<MaquinaResponseDTO> listar() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return List.of();
        return maquinaRepository.findAllByEmpresaId(empresaId).stream().map(this::toDTO).toList();
    }

    public MaquinaResponseDTO buscarPorId(Long id) {
        Maquina m = maquinaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + id));
        requireSameTenant(m.getEmpresa());
        return toDTO(m);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "maquinas", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public MaquinaResponseDTO atualizar(Long id, MaquinaRequestDTO dto) {
        Maquina m = maquinaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + id));
        requireSameTenant(m.getEmpresa());
        applyDto(dto, m);
        log.info("Atualizando máquina id={}", id);
        return toDTO(maquinaRepository.save(m));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "maquinas", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public void deletar(Long id) {
        Maquina m = maquinaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + id));
        requireSameTenant(m.getEmpresa());
        log.info("Deletando máquina id={}", id);
        maquinaRepository.deleteById(id);
    }

    private void applyDto(MaquinaRequestDTO dto, Maquina m) {
        m.setNome(dto.nome());
        m.setSetor(dto.setor());
        m.setStatus(dto.status());
        m.setPrioridade(dto.prioridade() != null ? dto.prioridade() : (m.getPrioridade() != null ? m.getPrioridade() : "MEDIA"));
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
        if (m.getIntervaloPreventivaDias() == 0) return false;
        if (m.getDataUltimaManutencao() == null) return true;
        return m.getDataUltimaManutencao().plusDays(m.getIntervaloPreventivaDias()).isBefore(LocalDate.now());
    }

    private void requireSameTenant(Empresa empresa) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return; // scheduler/system context
        if (empresa == null || !empresaId.equals(empresa.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
    }
}
