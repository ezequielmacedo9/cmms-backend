package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.DashboardStatsDTO;
import br.com.cmms.cmms.dto.NotificationDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Unit tests for {@link NotificationService} (severity mapping of overdue alerts). */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock DashboardService dashboardService;
    @InjectMocks NotificationService service;

    @Test
    @DisplayName("mapeia alertas vencidos com severidade por dias e prioridade")
    void currentForUser_mapeiaSeveridades() {
        when(dashboardService.getStats()).thenReturn(statsCom(List.of(
            alert(1L, "Torno",      2,  "CRITICA"), // prioridade CRITICA -> CRITICAL
            alert(2L, "Fresa",      45, "MEDIA"),   // >=30 dias -> CRITICAL
            alert(3L, "Prensa",     10, "MEDIA"),   // >=7 dias -> HIGH
            alert(4L, "Compressor", 2,  "ALTA"),    // prioridade ALTA -> HIGH
            alert(5L, "Esteira",    2,  "BAIXA")    // resto -> MEDIUM
        )));

        List<NotificationDTO> out = service.currentForUser();

        assertThat(out).hasSize(5);
        assertThat(out).extracting(NotificationDTO::severity)
            .containsExactly("CRITICAL", "CRITICAL", "HIGH", "HIGH", "MEDIUM");
        assertThat(out.get(0).id()).isEqualTo("OVERDUE-1");
        assertThat(out.get(0).message()).contains("Torno").contains("dias em atraso");
        assertThat(out.get(0).link()).isEqualTo("/maquinas");
    }

    @Test
    @DisplayName("sem alertas vencidos devolve feed vazio")
    void currentForUser_vazio() {
        when(dashboardService.getStats()).thenReturn(statsCom(List.of()));
        assertThat(service.currentForUser()).isEmpty();
    }

    private static DashboardStatsDTO statsCom(List<DashboardStatsDTO.OverdueAlert> alertas) {
        return new DashboardStatsDTO(0, 0, 0, 0, 0, 0, 0, 0, alertas.size(),
            100.0, 0.0, List.of(), alertas);
    }

    private static DashboardStatsDTO.OverdueAlert alert(Long id, String nome, long dias, String prioridade) {
        return new DashboardStatsDTO.OverdueAlert(id, nome, "Setor A", dias, prioridade);
    }
}
