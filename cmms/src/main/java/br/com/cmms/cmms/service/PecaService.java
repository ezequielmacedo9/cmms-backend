package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.PecaRequestDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.PecaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PecaService {

    @Autowired
    private PecaRepository pecaRepository;

    public PecaResponseDTO cadastrar(PecaRequestDTO dto) {
        Peca peca = new Peca();

        peca.setNome(dto.getNome());
        peca.setCodigo(dto.getCodigo());
        peca.setQuantidadeEmEstoque(dto.getQuantidadeEmEstoque());
        peca.setCustoUnitario(dto.getCustoUnitario());
        peca.setVidaUtilHoras(dto.getVidaUtilHoras());

        Peca salva = pecaRepository.save(peca);

        return new PecaResponseDTO(salva);
    }


    // READ ALL
    public List<PecaResponseDTO> listar() {
        return pecaRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // READ BY ID
    public PecaResponseDTO buscarPorId(Long id) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Peça não encontrada"));
        return toResponseDTO(peca);
    }

    // UPDATE
    public PecaResponseDTO atualizar(Long id, PecaRequestDTO dto) {
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Peça não encontrada"));

        copiarDtoParaEntity(dto, peca);
        Peca atualizada = pecaRepository.save(peca);

        return toResponseDTO(atualizada);
    }

    // DELETE
    public void deletar(Long id) {
        if (!pecaRepository.existsById(id)) {
            throw new RuntimeException("Peça não encontrada");
        }
        pecaRepository.deleteById(id);
    }



    private void copiarDtoParaEntity(PecaRequestDTO dto, Peca peca) {
        peca.setNome(dto.getNome());
        peca.setCodigo(dto.getCodigo());
        peca.setQuantidadeEmEstoque(dto.getQuantidadeEmEstoque());
        peca.setCustoUnitario(dto.getCustoUnitario());
        peca.setVidaUtilHoras(dto.getVidaUtilHoras());
    }

    private PecaResponseDTO toResponseDTO(Peca peca) {
        PecaResponseDTO dto = new PecaResponseDTO();
        dto.setId(peca.getId());
        dto.setNome(peca.getNome());
        dto.setCodigo(peca.getCodigo());
        dto.setQuantidadeEmEstoque(peca.getQuantidadeEmEstoque());
        dto.setCustoUnitario(peca.getCustoUnitario());
        dto.setVidaUtilHoras(peca.getVidaUtilHoras());
        return dto;
    }
}
