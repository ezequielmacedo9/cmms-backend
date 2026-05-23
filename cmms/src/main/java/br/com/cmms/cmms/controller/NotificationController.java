package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.NotificationDTO;
import br.com.cmms.cmms.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint para o feed de notificações que o frontend faz polling a
 * cada 60s. Mantido leve — sem paginação, sem filtros: a lista cabe em
 * memória por design.
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificações")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Notificações atuais para o usuário autenticado",
        description = "Retorna alertas operacionais (preventivas vencidas, etc). "
                    + "Cacheado por 60s no backend.")
    public ResponseEntity<List<NotificationDTO>> current() {
        return ResponseEntity.ok(service.currentForUser());
    }
}
