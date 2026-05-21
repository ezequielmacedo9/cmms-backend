package br.com.cmms.cmms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload de entrada para criar / atualizar uma máquina.
 *
 * <p>Validações centralizadas em {@link Constraints}.
 */
@Schema(description = "Dados para cadastro ou atualização de uma máquina.")
public record MaquinaRequestDTO(

    @NotBlank(message = "Nome é obrigatório.")
    @Size(min = 2, max = Constraints.NOME_MAX,
        message = "Nome deve ter entre 2 e " + Constraints.NOME_MAX + " caracteres.")
    @Schema(example = "Torno CNC 02", maxLength = Constraints.NOME_MAX)
    String nome,

    @NotBlank(message = "Setor é obrigatório.")
    @Size(min = 2, max = 80, message = "Setor deve ter entre 2 e 80 caracteres.")
    @Schema(example = "Usinagem", maxLength = 80)
    String setor,

    @NotBlank(message = "Status é obrigatório.")
    @Pattern(regexp = Constraints.MAQUINA_STATUS_REGEX,
        message = "Status deve ser ATIVO, INATIVO ou EM_MANUTENCAO.")
    @Schema(example = "ATIVO", allowableValues = {"ATIVO", "INATIVO", "EM_MANUTENCAO"})
    String status,

    @Pattern(regexp = Constraints.PRIORIDADE_REGEX,
        message = "Prioridade deve ser CRITICA, ALTA, MEDIA ou BAIXA.")
    @Schema(example = "MEDIA", allowableValues = {"CRITICA", "ALTA", "MEDIA", "BAIXA"})
    String prioridade,

    @PositiveOrZero(message = "Intervalo preventiva deve ser >= 0.")
    @Max(value = 3650, message = "Intervalo preventiva acima de 10 anos não faz sentido.")
    @Schema(example = "30",
        description = "Dias entre manutenções preventivas. 0 = sem preventiva agendada.")
    Integer intervaloPreventivaDias,

    @Schema(example = "2026-05-15")
    LocalDate dataUltimaManutencao
) {}
