package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Ferramenta;
import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManutencaoService {

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private MaquinaRepository maquinaRepository;

    public Manutencao cadastrar(Manutencao manutencao, Long maquinaId){
        Maquina maquina = maquinaRepository.findById(maquinaId).orElse(null);
        if(maquina != null){
            manutencao.setMaquina(maquina);
            // opcional: adicionar na lista da máquina
            maquina.getListaDeManutencoes().add(manutencao);
        }
        return manutencaoRepository.save(manutencao);
    }

    public List<Manutencao> listar(){
        return manutencaoRepository.findAll();
    }

    public Manutencao buscarPorId(Long id){
        return manutencaoRepository.findById(id).orElse(null);
    }

    public Manutencao atualizar(Manutencao manutencao){
        return manutencaoRepository.save(manutencao);
    }

    public void deletar(Long id){
        manutencaoRepository.deleteById(id);
    }

    public void registrar(Maquina maquinaSelecionada, String preventiva,
                          String joão, String trocaDeCorreia,
                          List<Peca> pecas, List<Ferramenta> ferramentas) {
    }
}
