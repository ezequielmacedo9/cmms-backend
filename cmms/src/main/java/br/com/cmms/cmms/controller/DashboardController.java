package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.DashboardStatsDTO;
import br.com.cmms.cmms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @Operation(summary = "KPIs do dashboard",
        description = "Totais por status, MTBF, série histórica dos últimos 6 meses e alertas de preventivas vencidas.")
    public ResponseEntity<DashboardStatsDTO> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
