package br.com.cmms.cmms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight notification sent to the frontend's polling loop.
 *
 * <p>The current source of notifications is the dashboard's overdue
 * preventive maintenance list — each overdue machine produces one
 * notification. Future sources (stock-out, lockouts) will use the same
 * envelope.
 */
@Schema(description = "Notificação operacional para o usuário.")
public record NotificationDTO(

    @Schema(description = "ID estável da notificação. Usado como key no frontend.",
        example = "OVERDUE-7")
    String id,

    @Schema(description = "Categoria semântica — drives icon and color no UI.",
        allowableValues = {"OVERDUE_MAINTENANCE", "INFO", "WARNING", "ERROR"})
    String type,

    @Schema(description = "Severidade. Frontend pode escolher exibir só CRITICAL/HIGH.",
        allowableValues = {"CRITICAL", "HIGH", "MEDIUM", "LOW"})
    String severity,

    @Schema(description = "Título curto para o toast / lista.",
        example = "Preventiva vencida")
    String title,

    @Schema(description = "Mensagem detalhada em pt-BR.",
        example = "Torno CNC 02 (Usinagem) — 12 dias em atraso.")
    String message,

    @Schema(description = "Rota relativa para resolução. Pode ser nula.",
        example = "/maquinas")
    String link
) {}
