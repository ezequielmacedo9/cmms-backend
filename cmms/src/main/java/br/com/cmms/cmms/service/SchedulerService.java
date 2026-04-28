package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PasswordResetTokenRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final MaquinaRepository maquinaRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ManutencaoService manutencaoService;
    private final PecaService pecaService;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.self.url:http://localhost:8080}")
    private String selfUrl;

    public SchedulerService(MaquinaRepository maquinaRepository,
                            PasswordResetTokenRepository passwordResetTokenRepository,
                            ManutencaoService manutencaoService,
                            PecaService pecaService,
                            EmailService emailService,
                            UsuarioRepository usuarioRepository) {
        this.maquinaRepository = maquinaRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.manutencaoService = manutencaoService;
        this.pecaService = pecaService;
        this.emailService = emailService;
        this.usuarioRepository = usuarioRepository;
    }

    /** Hourly: auto-create OS for overdue preventives and email admins */
    @Scheduled(cron = "0 0 * * * *")
    @CacheEvict(value = "dashboard-stats", allEntries = true)
    public void verificarManutencoesVencidas() {
        int geradas = manutencaoService.gerarPreventivasVencidas();
        if (geradas > 0) {
            log.info("Scheduler: {} preventive OS generated", geradas);
        }

        LocalDate hoje = LocalDate.now();
        List<Maquina> vencidas = maquinaRepository.findByIntervaloPreventivaDiasGreaterThan(0).stream()
            .filter(m -> {
                if (m.getDataUltimaManutencao() == null) return true;
                return m.getDataUltimaManutencao().plusDays(m.getIntervaloPreventivaDias()).isBefore(hoje);
            })
            .toList();

        if (vencidas.isEmpty()) {
            log.info("Scheduler: no overdue preventive maintenances");
            return;
        }

        log.warn("Scheduler: {} machine(s) with overdue maintenance — {}",
            vencidas.size(),
            vencidas.stream().map(Maquina::getNome).collect(Collectors.joining(", ")));

        List<String> adminEmails = getAdminEmails();
        if (adminEmails.isEmpty()) return;

        for (Maquina maquina : vencidas) {
            long diasVencido;
            if (maquina.getDataUltimaManutencao() == null) {
                diasVencido = maquina.getIntervaloPreventivaDias();
            } else {
                LocalDate vencimento = maquina.getDataUltimaManutencao()
                    .plusDays(maquina.getIntervaloPreventivaDias());
                diasVencido = ChronoUnit.DAYS.between(vencimento, hoje);
            }
            String setor = maquina.getSetor() != null ? maquina.getSetor() : "—";
            for (String email : adminEmails) {
                emailService.sendManutencaoVencida(email, maquina.getNome(), setor, diasVencido);
            }
        }
    }

    /** Daily at 8AM: alert admins about open maintenances with SLA expiring within 1 day */
    @Scheduled(cron = "0 0 8 * * *")
    public void verificarSlaVencendo() {
        LocalDate hoje = LocalDate.now();
        LocalDate amanha = hoje.plusDays(1);

        List<ManutencaoResponseDTO> proximas = manutencaoService.listar().stream()
            .filter(m -> !"CONCLUIDA".equalsIgnoreCase(m.status()))
            .filter(m -> m.prazoSla() != null && !m.prazoSla().isAfter(amanha))
            .toList();

        if (proximas.isEmpty()) {
            log.info("Scheduler: no SLA deadlines approaching");
            return;
        }

        List<String> adminEmails = getAdminEmails();
        if (adminEmails.isEmpty()) return;

        for (ManutencaoResponseDTO m : proximas) {
            long diasRestantes = ChronoUnit.DAYS.between(hoje, m.prazoSla());
            String maquinaNome = m.maquina() != null ? m.maquina().nome() : "—";
            for (String email : adminEmails) {
                emailService.sendSlaVencendo(email, maquinaNome, m.tipo(), diasRestantes);
            }
        }
        log.info("Scheduler: SLA alerts sent for {} maintenance(s)", proximas.size());
    }

    /** Daily at 9AM: alert admins about parts below minimum stock level */
    @Scheduled(cron = "0 0 9 * * *")
    public void verificarEstoqueBaixo() {
        List<PecaResponseDTO> baixoEstoque = pecaService.listarBaixoEstoque();

        if (baixoEstoque.isEmpty()) {
            log.info("Scheduler: stock levels OK");
            return;
        }

        List<String> adminEmails = getAdminEmails();
        if (adminEmails.isEmpty()) return;

        for (PecaResponseDTO peca : baixoEstoque) {
            int qtdAtual = peca.getQuantidadeEmEstoque() != null ? peca.getQuantidadeEmEstoque() : 0;
            int qtdMinima = peca.getQuantidadeMinima() != null ? peca.getQuantidadeMinima() : 0;
            for (String email : adminEmails) {
                emailService.sendEstoqueBaixo(email, peca.getNome(), peca.getCodigo(), qtdAtual, qtdMinima);
            }
        }
        log.info("Scheduler: low stock alerts sent for {} part(s)", baixoEstoque.size());
    }

    /** Every 10 minutes: self-ping to prevent Render free tier from sleeping */
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
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

    private List<String> getAdminEmails() {
        return usuarioRepository.findAll().stream()
            .filter(u -> u.isAtivo() && u.getRole() != null)
            .filter(u -> {
                String r = u.getRole().getNome();
                return "ROLE_SUPER_ADMIN".equals(r) || "ROLE_ADMIN".equals(r) || "ROLE_GESTOR".equals(r);
            })
            .map(u -> u.getEmail())
            .toList();
    }
}
