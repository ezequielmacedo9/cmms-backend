package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final MaquinaRepository maquinaRepository;

    public SchedulerService(MaquinaRepository maquinaRepository) {
        this.maquinaRepository = maquinaRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @CacheEvict(value = "dashboard-stats", allEntries = true)
    public void verificarManutencoesVencidas() {
        LocalDate hoje = LocalDate.now();
        List<Maquina> vencidas = maquinaRepository.findByIntervaloPreventivaDiasGreaterThan(0).stream()
            .filter(m -> {
                if (m.getDataUltimaManutencao() == null) return true;
                return m.getDataUltimaManutencao().plusDays(m.getIntervaloPreventivaDias()).isBefore(hoje);
            })
            .toList();

        if (vencidas.isEmpty()) {
            log.info("Verificação horária: nenhuma manutenção preventiva vencida");
        } else {
            log.warn("Verificação horária: {} máquina(s) com preventiva vencida — {}",
                vencidas.size(),
                vencidas.stream().map(Maquina::getNome).collect(Collectors.joining(", ")));
        }
    }
}
