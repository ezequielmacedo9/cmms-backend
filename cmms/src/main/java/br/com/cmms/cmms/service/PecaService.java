package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.PecaRequestDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.PecaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PecaService {

    private static final Logger log = LoggerFactory.getLogger(PecaService.class);

    private final PecaRepository pecaRepository;
    private final TenantResolver tenant;

    public PecaService(PecaRepository pecaRepository, TenantResolver tenant) {
        this.pecaRepository = pecaRepository;
        this.tenant = tenant;
    }

    @Transactional
    public PecaResponseDTO cadastrar(PecaRequestDTO dto) {
        Peca peca = new Peca();
        peca.setEmpresaId(tenant.requireEmpresaId());
        copiarDtoParaEntity(dto, peca);
        log.info("Cadastrando peça: {}", dto.getNome());
        return toResponseDTO(pecaRepository.save(peca));
    }

    public List<PecaResponseDTO> listar() {
        return pecaRepository.findByEmpresaId(tenant.requireEmpresaId())
            .stream().map(this::toResponseDTO).toList();
    }

    public Page<PecaResponseDTO> listar(String q, Pageable pageable) {
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim();
        return pecaRepository.search(normalizedQ, tenant.requireEmpresaId(), pageable).map(this::toResponseDTO);
    }

    public PecaResponseDTO buscarPorId(Long id) {
        return toResponseDTO(findOwned(id));
    }

    @Transactional
    public PecaResponseDTO atualizar(Long id, PecaRequestDTO dto) {
        Peca peca = findOwned(id);
        copiarDtoParaEntity(dto, peca);
        log.info("Atualizando peça id={}", id);
        return toResponseDTO(pecaRepository.save(peca));
    }

    @Transactional
    public void deletar(Long id) {
        Peca peca = findOwned(id);
        log.info("Deletando peça id={}", id);
        pecaRepository.delete(peca);
    }

    private Peca findOwned(Long id) {
        return pecaRepository.findByIdAndEmpresaId(id, tenant.requireEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Peça", id));
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
