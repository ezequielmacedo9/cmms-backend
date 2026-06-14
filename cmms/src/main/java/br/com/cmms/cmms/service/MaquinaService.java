package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.MaquinaRequestDTO;
import br.com.cmms.cmms.dto.MaquinaResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Maquina;
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
 * Machine CRUD, fully scoped to the caller's empresa. Every read/write
 * resolves the tenant through {@link TenantResolver} so cross-tenant access
 * is impossible — there is no code path that returns another empresa's rows.
 */
@Service
public class MaquinaService {

    private static final Logger log = LoggerFactory.getLogger(MaquinaService.class);

    private final MaquinaRepository maquinaRepository;
    private final TenantResolver tenant;
    private final AssinaturaService assinatura;

    public MaquinaService(MaquinaRepository maquinaRepository, TenantResolver tenant,
                          AssinaturaService assinatura) {
        this.maquinaRepository = maquinaRepository;
        this.tenant = tenant;
        this.assinatura = assinatura;
    }

    @Transactional
    public MaquinaResponseDTO cadastrar(MaquinaRequestDTO dto) {
        Long empresaId = tenant.requireEmpresaId();
        assinatura.assertPodeCriarMaquina(empresaId); // plan-quota gate
        Maquina m = new Maquina();
        m.setEmpresaId(empresaId);
        applyDto(dto, m);
        log.info("Cadastrando máquina: {}", dto.nome());
        return toDTO(maquinaRepository.save(m));
    }

    /** Paged listing with optional search and status filter, scoped to the empresa. */
    public Page<MaquinaResponseDTO> listar(String q, String status, Pageable pageable) {
        String normalizedQ      = (q == null      || q.isBlank())      ? null : q.trim();
        String normalizedStatus = (status == null || status.isBlank()) ? null : status.trim();
        return maquinaRepository.search(normalizedQ, normalizedStatus, tenant.requireEmpresaId(), pageable)
            .map(this::toDTO);
    }

    /** Legacy non-paged listing — scoped to the empresa. */
    public List<MaquinaResponseDTO> listar() {
        return maquinaRepository.findByEmpresaId(tenant.requireEmpresaId())
            .stream().map(this::toDTO).toList();
    }

    public MaquinaResponseDTO buscarPorId(Long id) {
        return toDTO(findOwned(id));
    }

    @Transactional
    public MaquinaResponseDTO atualizar(Long id, MaquinaRequestDTO dto) {
        Maquina m = findOwned(id);
        applyDto(dto, m);
        log.info("Atualizando máquina id={}", id);
        return toDTO(maquinaRepository.save(m));
    }

    /**
     * Soft delete: stamps {@code deleted_at} so the row is filtered out of
     * every subsequent query. Auditing keeps the row, allowing recovery.
     */
    @Transactional
    public void deletar(Long id) {
        Maquina m = findOwned(id);
        log.info("Soft-deleting máquina id={}", id);
        m.setDeletedAt(LocalDateTime.now());
        maquinaRepository.save(m);
    }

    /** Tenant-scoped fetch-or-404. Returning 404 (not 403) avoids leaking that the id exists elsewhere. */
    private Maquina findOwned(Long id) {
        return maquinaRepository.findByIdAndEmpresaId(id, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Máquina", id));
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
