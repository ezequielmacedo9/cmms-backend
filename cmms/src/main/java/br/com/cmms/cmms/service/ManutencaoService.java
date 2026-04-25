package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManutencaoService {

    private static final Logger log = LoggerFactory.getLogger(ManutencaoService.class);

    private final ManutencaoRepository manutencaoRepository;
    private final MaquinaRepository maquinaRepository;

    public ManutencaoService(ManutencaoRepository manutencaoRepository,
                             MaquinaRepository maquinaRepository) {
        this.manutencaoRepository = manutencaoRepository;
        this.maquinaRepository = maquinaRepository;
    }

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
    public void deletar(Long id) {
        if (!manutencaoRepository.existsById(id)) {
            throw new RuntimeException("Manutenção não encontrada: " + id);
        }
        log.info("Deletando manutenção id={}", id);
        manutencaoRepository.deleteById(id);
    }
}
