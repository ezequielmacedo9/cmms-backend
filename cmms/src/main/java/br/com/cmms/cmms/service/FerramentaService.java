package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.FerramentaRequestDTO;
import br.com.cmms.cmms.dto.FerramentaResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Ferramenta;
import br.com.cmms.cmms.repository.FerramentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service over the {@link Ferramenta} aggregate. Exposes DTOs only;
 * entities never cross the service boundary.
 */
@Service
public class FerramentaService {

    private final FerramentaRepository ferramentaRepository;

    public FerramentaService(FerramentaRepository ferramentaRepository) {
        this.ferramentaRepository = ferramentaRepository;
    }

    @Transactional
    public FerramentaResponseDTO cadastrar(FerramentaRequestDTO dto) {
        Ferramenta f = new Ferramenta();
        applyDto(dto, f);
        return FerramentaResponseDTO.from(ferramentaRepository.save(f));
    }

    public List<FerramentaResponseDTO> listar() {
        return ferramentaRepository.findAll().stream()
            .map(FerramentaResponseDTO::from)
            .toList();
    }

    public FerramentaResponseDTO buscarPorId(Long id) {
        return FerramentaResponseDTO.from(findOrThrow(id));
    }

    @Transactional
    public FerramentaResponseDTO atualizar(Long id, FerramentaRequestDTO dto) {
        Ferramenta f = findOrThrow(id);
        applyDto(dto, f);
        return FerramentaResponseDTO.from(ferramentaRepository.save(f));
    }

    @Transactional
    public void deletar(Long id) {
        if (!ferramentaRepository.existsById(id)) {
            throw NotFoundException.of("Ferramenta", id);
        }
        ferramentaRepository.deleteById(id);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Ferramenta findOrThrow(Long id) {
        return ferramentaRepository.findById(id)
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
