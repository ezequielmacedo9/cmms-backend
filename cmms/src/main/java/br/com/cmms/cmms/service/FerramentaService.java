package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Ferramenta;
import br.com.cmms.cmms.repository.FerramentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FerramentaService {

    @Autowired
    private FerramentaRepository ferramentaRepository;

    // Cadastrar
    public Ferramenta cadastrar(Ferramenta ferramenta){
        return ferramentaRepository.save(ferramenta);
    }

    // Listar todas
    public List<Ferramenta> listar(){
        return ferramentaRepository.findAll();
    }

    // Buscar por ID
    public Ferramenta buscarPorId(Long id){
        return ferramentaRepository.findById(id).orElse(null);
    }

    // Atualizar
    public Ferramenta atualizar(Long id, Ferramenta ferramenta) {
        Ferramenta existente = ferramentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ferramenta não encontrada"));

        existente.setNome(ferramenta.getNome());

        return ferramentaRepository.save(existente);
    }

    // Deletar
    public void deletar(Long id){
        ferramentaRepository.deleteById(id);
    }
}
