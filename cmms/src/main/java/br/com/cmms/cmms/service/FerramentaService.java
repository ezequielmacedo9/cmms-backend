package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.FerramentaRequestDTO;
import br.com.cmms.cmms.dto.FerramentaResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Ferramenta;
import br.com.cmms.cmms.repository.FerramentaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FerramentaService {

    private final FerramentaRepository ferramentaRepository;
    private final TenantResolver tenant;

    public FerramentaService(FerramentaRepository ferramentaRepository, TenantResolver tenant) {
        this.ferramentaRepository = ferramentaRepository;
        this.tenant = tenant;
    }

    @Transactional
    public FerramentaResponseDTO cadastrar(FerramentaRequestDTO dto) {
        Ferramenta f = new Ferramenta();
        f.setEmpresaId(tenant.requireEmpresaId());
        applyDto(dto, f);
        return FerramentaResponseDTO.from(ferramentaRepository.save(f));
    }

    public List<FerramentaResponseDTO> listar() {
        return ferramentaRepository.findByEmpresaId(tenant.requireEmpresaId()).stream()
            .map(FerramentaResponseDTO::from)
            .toList();
    }

    public Page<FerramentaResponseDTO> listar(String q, Pageable pageable) {
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim();
        return ferramentaRepository.search(normalizedQ, tenant.requireEmpresaId(), pageable)
            .map(FerramentaResponseDTO::from);
    }

    public FerramentaResponseDTO buscarPorId(Long id) {
        return FerramentaResponseDTO.from(findOwned(id));
    }

    @Transactional
    public FerramentaResponseDTO atualizar(Long id, FerramentaRequestDTO dto) {
        Ferramenta f = findOwned(id);
        applyDto(dto, f);
        return FerramentaResponseDTO.from(ferramentaRepository.save(f));
    }

    @Transactional
    public void deletar(Long id) {
        Ferramenta f = findOwned(id);
        ferramentaRepository.delete(f);
    }

    private Ferramenta findOwned(Long id) {
        return ferramentaRepository.findByIdAndEmpresaId(id, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Ferramenta", id));
    }

    private void applyDto(FerramentaRequestDTO dto, Ferramenta f) {
        f.setNome(dto.nome());
        f.setCodigo(dto.codigo());
        f.setStatus(dto.status());
        f.setLocalizacao(dto.localizacao());
        f.setResponsavel(dto.responsavel());
        f.setDataUltimaManutencao(dto.dataUltimaManutencao());
    }
}
