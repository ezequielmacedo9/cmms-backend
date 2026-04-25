package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaquinaService {

    private static final Logger log = LoggerFactory.getLogger(MaquinaService.class);

    private final MaquinaRepository maquinaRepository;

    public MaquinaService(MaquinaRepository maquinaRepository) {
        this.maquinaRepository = maquinaRepository;
    }

    @Transactional
    public Maquina cadastrar(Maquina maquina) {
        log.info("Cadastrando máquina: {}", maquina.getNome());
        return maquinaRepository.save(maquina);
    }

    public List<Maquina> listar() {
        return maquinaRepository.findAll();
    }

    public Maquina buscarPorId(Long id) {
        return maquinaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + id));
    }

    @Transactional
    public Maquina atualizar(Long id, Maquina maquina) {
        Maquina existente = maquinaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + id));

        existente.setNome(maquina.getNome());
        existente.setSetor(maquina.getSetor());
        existente.setStatus(maquina.getStatus());
        existente.setIntervaloPreventivaDias(maquina.getIntervaloPreventivaDias());
        existente.setDataUltimaManutencao(maquina.getDataUltimaManutencao());

        log.info("Atualizando máquina id={}", id);
        return maquinaRepository.save(existente);
    }

    @Transactional
    public void deletar(Long id) {
        if (!maquinaRepository.existsById(id)) {
            throw new RuntimeException("Máquina não encontrada: " + id);
        }
        log.info("Deletando máquina id={}", id);
        maquinaRepository.deleteById(id);
    }
}
