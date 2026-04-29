package br.com.cmms.cmms.service;

import br.com.cmms.cmms.Security.TenantContext;
import br.com.cmms.cmms.model.Empresa;
import br.com.cmms.cmms.model.Ferramenta;
import br.com.cmms.cmms.repository.EmpresaRepository;
import br.com.cmms.cmms.repository.FerramentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FerramentaService {

    private final FerramentaRepository ferramentaRepository;
    private final EmpresaRepository empresaRepository;

    public FerramentaService(FerramentaRepository ferramentaRepository, EmpresaRepository empresaRepository) {
        this.ferramentaRepository = ferramentaRepository;
        this.empresaRepository = empresaRepository;
    }

    public Ferramenta cadastrar(Ferramenta ferramenta) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            ferramenta.setEmpresa(empresaRepository.getReferenceById(empresaId));
        }
        return ferramentaRepository.save(ferramenta);
    }

    public List<Ferramenta> listar() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return List.of();
        return ferramentaRepository.findAllByEmpresaId(empresaId);
    }

    public Ferramenta buscarPorId(Long id) {
        Ferramenta f = ferramentaRepository.findById(id).orElse(null);
        if (f != null) requireSameTenant(f.getEmpresa());
        return f;
    }

    public Ferramenta atualizar(Long id, Ferramenta ferramenta) {
        Ferramenta existente = ferramentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ferramenta não encontrada"));
        requireSameTenant(existente.getEmpresa());
        existente.setNome(ferramenta.getNome());
        return ferramentaRepository.save(existente);
    }

    public void deletar(Long id) {
        Ferramenta f = ferramentaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ferramenta não encontrada"));
        requireSameTenant(f.getEmpresa());
        ferramentaRepository.deleteById(id);
    }

    private void requireSameTenant(Empresa empresa) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return;
        if (empresa == null || !empresaId.equals(empresa.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
    }
}
