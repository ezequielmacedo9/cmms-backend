package br.com.cmms.cmms.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ManutencaoRequestDTO(
    @NotBlank(message = "Tipo é obrigatório") String tipo,
    @NotBlank(message = "Técnico é obrigatório") String tecnico,
    String descricao,
    String prioridade,
    String status,
    LocalDate dataManutencao
) {}
