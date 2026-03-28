package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManutencaoService {

    private static final Logger log = LoggerFactory.getLogger(ManutencaoService.class);

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private MaquinaRepository maquinaRepository;

    @Transactional
    public Manutencao cadastrar(Manutencao manutencao, Long maquinaId) {
        Maquina maquina = maquinaRepository.findById(maquinaId)
            .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + maquinaId));
        manutencao.setMaquina(maquina);
        log.info("Registrando manutenção tipo={} para máquina id={}", manutencao.getTipo(), maquinaId);
        return manutencaoRepository.save(manutencao);
    }

    public List<Manutencao> listar() {
        return manutencaoRepository.findAll();
    }

    public List<Manutencao> listarPorMaquina(Long maquinaId) {
        return manutencaoRepository.findByMaquinaIdOrderByDataManutencaoDesc(maquinaId);
    }

    public Manutencao buscarPorId(Long id) {
        return manutencaoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Manutenção não encontrada: " + id));
    }

    @Transactional
    public Manutencao atualizar(Manutencao manutencao) {
        return manutencaoRepository.save(manutencao);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando manutenção id={}", id);
        manutencaoRepository.deleteById(id);
    }
}
