package br.com.cmms.cmms.service;

import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Ferramenta;
import br.com.cmms.cmms.repository.FerramentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lightweight service over the {@link Ferramenta} aggregate.
 *
 * <p>Will be expanded in FASE 2B-7 with DTO-based methods. For now it just
 * wraps the repository, but already uses constructor injection and the new
 * exception hierarchy so the rest of the codebase can reference it safely.
 */
@Service
public class FerramentaService {

    private final FerramentaRepository ferramentaRepository;

    public FerramentaService(FerramentaRepository ferramentaRepository) {
        this.ferramentaRepository = ferramentaRepository;
    }

    @Transactional
    public Ferramenta cadastrar(Ferramenta ferramenta) {
        return ferramentaRepository.save(ferramenta);
    }

    public List<Ferramenta> listar() {
        return ferramentaRepository.findAll();
    }

    public Ferramenta buscarPorId(Long id) {
        return ferramentaRepository.findById(id)
            .orElseThrow(() -> NotFoundException.of("Ferramenta", id));
    }

    @Transactional
    public Ferramenta atualizar(Long id, Ferramenta ferramenta) {
        Ferramenta existente = ferramentaRepository.findById(id)
            .orElseThrow(() -> NotFoundException.of("Ferramenta", id));
        existente.setNome(ferramenta.getNome());
        return ferramentaRepository.save(existente);
    }

    @Transactional
    public void deletar(Long id) {
        if (!ferramentaRepository.existsById(id)) {
            throw NotFoundException.of("Ferramenta", id);
        }
        ferramentaRepository.deleteById(id);
    }
}
