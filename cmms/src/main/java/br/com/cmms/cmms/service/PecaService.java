package br.com.cmms.cmms.service;

import br.com.cmms.cmms.Security.TenantContext;
import br.com.cmms.cmms.dto.PecaRequestDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.model.Empresa;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.EmpresaRepository;
import br.com.cmms.cmms.repository.PecaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PecaService {

    private static final Logger log = LoggerFactory.getLogger(PecaService.class);

    private final PecaRepository pecaRepository;
    private final EmpresaRepository empresaRepository;

    public PecaService(PecaRepository pecaRepository, EmpresaRepository empresaRepository) {
        this.pecaRepository = pecaRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public PecaResponseDTO cadastrar(PecaRequestDTO dto) {
        Peca peca = new Peca();
        copiarDtoParaEntity(dto, peca);
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            peca.setEmpresa(empresaRepository.getReferenceById(empresaId));
        }
        log.info("Cadastrando peça: {} (empresa={})", dto.getNome(), empresaId);
        return toResponseDTO(pecaRepository.save(peca));
    }

    public List<PecaResponseDTO> listar() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return List.of();
        return pecaRepository.findAllByEmpresaId(empresaId).stream().map(this::toResponseDTO).toList();
    }

    public PecaResponseDTO buscarPorId(Long id) {
        Peca peca = pecaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Peça não encontrada: " + id));
        requireSameTenant(peca.getEmpresa());
        return toResponseDTO(peca);
    }

    @Transactional
    public PecaResponseDTO atualizar(Long id, PecaRequestDTO dto) {
        Peca peca = pecaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Peça não encontrada: " + id));
        requireSameTenant(peca.getEmpresa());
        copiarDtoParaEntity(dto, peca);
        log.info("Atualizando peça id={}", id);
        return toResponseDTO(pecaRepository.save(peca));
    }

    @Transactional
    public void deletar(Long id) {
        Peca peca = pecaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Peça não encontrada: " + id));
        requireSameTenant(peca.getEmpresa());
        log.info("Deletando peça id={}", id);
        pecaRepository.deleteById(id);
    }

    public List<PecaResponseDTO> listarBaixoEstoque() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return List.of();
        return pecaRepository.findAllByEmpresaId(empresaId).stream()
            .filter(Peca::isAbaixoDoMinimo)
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

    private void requireSameTenant(Empresa empresa) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return;
        if (empresa == null || !empresaId.equals(empresa.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
    }
}
