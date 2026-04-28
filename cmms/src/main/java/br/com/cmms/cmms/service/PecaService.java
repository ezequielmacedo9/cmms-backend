package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.PecaRequestDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.PecaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PecaService {

    private static final Logger log = LoggerFactory.getLogger(PecaService.class);

    private final PecaRepository pecaRepository;

    public PecaService(PecaRepository pecaRepository) {
        this.pecaRepository = pecaRepository;
    }

    @Transactional
    public PecaResponseDTO cadastrar(PecaRequestDTO dto) {
        Peca peca = new Peca();
        copiarDtoParaEntity(dto, peca);
        log.info("Cadastrando peça: {}", dto.getNome());
        return toResponseDTO(pecaRepository.save(peca));
    }

    public List<PecaResponseDTO> listar() {
        return pecaRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    public PecaResponseDTO buscarPorId(Long id) {
        return toResponseDTO(pecaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Peça não encontrada: " + id)));
    }

    @Transactional
    public PecaResponseDTO atualizar(Long id, PecaRequestDTO dto) {
        Peca peca = pecaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Peça não encontrada: " + id));
        copiarDtoParaEntity(dto, peca);
        log.info("Atualizando peça id={}", id);
        return toResponseDTO(pecaRepository.save(peca));
    }

    @Transactional
    public void deletar(Long id) {
        if (!pecaRepository.existsById(id)) {
            throw new RuntimeException("Peça não encontrada: " + id);
        }
        log.info("Deletando peça id={}", id);
        pecaRepository.deleteById(id);
    }

    public List<PecaResponseDTO> listarBaixoEstoque() {
        return pecaRepository.findAll().stream()
            .filter(p -> p.isAbaixoDoMinimo())
            .map(PecaResponseDTO::new)
            .toList();
    }

    private void copiarDtoParaEntity(PecaRequestDTO dto, Peca peca) {
        peca.setNome(dto.getNome());
        peca.setCodigo(dto.getCodigo());
        peca.setQuantidadeEmEstoque(dto.getQuantidadeEmEstoque());
        peca.setCustoUnitario(dto.getCustoUnitario());
        peca.setVidaUtilHoras(dto.getVidaUtilHoras());
        peca.setQuantidadeMinima(dto.getQuantidadeMinima());
    }

    private PecaResponseDTO toResponseDTO(Peca peca) {
        return new PecaResponseDTO(peca);
    }
}
