package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final MaquinaRepository maquinaRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.self.url:http://localhost:8080}")
    private String selfUrl;

    public SchedulerService(MaquinaRepository maquinaRepository,
                            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.maquinaRepository = maquinaRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /** Hourly: detect overdue preventive maintenances */
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
            log.info("Scheduler: no overdue preventive maintenances");
        } else {
            log.warn("Scheduler: {} machine(s) with overdue maintenance — {}",
                vencidas.size(),
                vencidas.stream().map(Maquina::getNome).collect(Collectors.joining(", ")));
        }
    }

    /** Every 14 minutes: self-ping to prevent Render free tier from sleeping */
    @Scheduled(fixedDelay = 840_000, initialDelay = 60_000)
    public void keepAlive() {
        try {
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> resp = rt.getForEntity(selfUrl + "/actuator/health", String.class);
            log.debug("Keep-alive ping: {}", resp.getStatusCode());
        } catch (Exception e) {
            log.debug("Keep-alive ping failed (ignoreable in dev): {}", e.getMessage());
        }
    }

    /** Daily at 3AM: clean up expired password reset tokens */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupTokens() {
        passwordResetTokenRepository.deleteExpiredAndUsed(LocalDateTime.now());
        log.info("Scheduler: expired password reset tokens cleaned up");
    }
}
