package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.DashboardStatsDTO;
import br.com.cmms.cmms.dto.NotificationDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the user-facing notification feed.
 *
 * <p>Current sources:
 * <ul>
 *   <li>Overdue preventive maintenances (from
 *       {@link DashboardService#getStats()}'s alerts list).</li>
 * </ul>
 *
 * <p>Cached for 60 seconds via the {@code notifications} cache so the
 * frontend polling loop (every 60s by default) doesn't recompute the
 * dashboard aggregates on every request.
 */
@Service
public class NotificationService {

    private static final int CRITICAL_THRESHOLD_DAYS = 30;
    private static final int HIGH_THRESHOLD_DAYS = 7;

    private final DashboardService dashboardService;

    public NotificationService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Cacheable("notifications")
    @Transactional(readOnly = true)
    public List<NotificationDTO> currentForUser() {
        DashboardStatsDTO stats = dashboardService.getStats();
        List<NotificationDTO> out = new ArrayList<>();

        for (DashboardStatsDTO.OverdueAlert alert : stats.alertasVencidos()) {
            String severity = severityForDays(alert.diasVencido(), alert.prioridade());
            out.add(new NotificationDTO(
                "OVERDUE-" + alert.maquinaId(),
                "OVERDUE_MAINTENANCE",
                severity,
                "Preventiva vencida",
                alert.maquinaNome() + " (" + alert.setor() + ") — "
                    + alert.diasVencido() + " dias em atraso.",
                "/maquinas"
            ));
        }

        return out;
    }

    private String severityForDays(long days, String priority) {
        // Priority can amplify the severity floor.
        if ("CRITICA".equalsIgnoreCase(priority)) return "CRITICAL";
        if (days >= CRITICAL_THRESHOLD_DAYS)      return "CRITICAL";
        if (days >= HIGH_THRESHOLD_DAYS)          return "HIGH";
        if ("ALTA".equalsIgnoreCase(priority))    return "HIGH";
        return "MEDIUM";
    }
}
