package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaquinaService {

    @Autowired
    private MaquinaRepository maquinaRepository;

    public Maquina cadastrar(Maquina maquina){
        return maquinaRepository.save(maquina);
    }

    public List<Maquina> listar(){
        return maquinaRepository.findAll();
    }

    public Maquina buscarPorId(Long id){
        return maquinaRepository.findById(id).orElse(null);
    }

    public Maquina atualizar(Long id, Maquina maquina){
        Maquina existente = maquinaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Máquina não encontrada"));

        existente.setNome(maquina.getNome());
        existente.setSetor(maquina.getSetor());
        existente.setStatus(maquina.getStatus());
        existente.setIntervaloPreventivaDias(maquina.getIntervaloPreventivaDias());
        existente.setDataUltimaManutencao(maquina.getDataUltimaManutencao());

        return maquinaRepository.save(existente);
    }

    public void deletar(Long id){
        maquinaRepository.deleteById(id);
    }
}
