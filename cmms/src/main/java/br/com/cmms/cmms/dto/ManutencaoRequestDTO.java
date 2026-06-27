package br.com.cmms.cmms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Dados para registrar uma ordem de serviço / manutenção em uma máquina.")
public record ManutencaoRequestDTO(

    @NotBlank(message = "Tipo é obrigatório.")
    @Pattern(regexp = Constraints.TIPO_MANUTENCAO_REGEX,
        message = "Tipo deve ser PREVENTIVA, CORRETIVA ou PREDITIVA.")
    @Schema(example = "CORRETIVA", allowableValues = {"PREVENTIVA", "CORRETIVA", "PREDITIVA"})
    String tipo,

    @NotBlank(message = "Técnico é obrigatório.")
    @Size(min = 2, max = Constraints.NOME_MAX,
        message = "Técnico deve ter entre 2 e " + Constraints.NOME_MAX + " caracteres.")
    @Schema(example = "Ezequiel Macedo", maxLength = Constraints.NOME_MAX)
    String tecnico,

    @Schema(description = "Id do usuário responsável (opcional).", example = "12")
    Long tecnicoId,

    @Size(max = Constraints.DESCRICAO_MAX,
        message = "Descrição limitada a " + Constraints.DESCRICAO_MAX + " caracteres.")
    @Schema(example = "Substituição do rolamento principal.", maxLength = Constraints.DESCRICAO_MAX)
    String descricao,

    @Pattern(regexp = Constraints.PRIORIDADE_REGEX,
        message = "Prioridade deve ser CRITICA, ALTA, MEDIA ou BAIXA.")
    @Schema(example = "ALTA", allowableValues = {"CRITICA", "ALTA", "MEDIA", "BAIXA"})
    String prioridade,

    @Pattern(regexp = Constraints.MANUTENCAO_STATUS_REGEX,
        message = "Status deve ser ABERTA, EM_ANDAMENTO, CONCLUIDA ou CANCELADA.")
    @Schema(example = "ABERTA",
        allowableValues = {"ABERTA", "EM_ANDAMENTO", "CONCLUIDA", "CANCELADA"})
    String status,

    @PastOrPresent(message = "Data da manutenção não pode estar no futuro.")
    @Schema(example = "2026-05-15")
    LocalDate dataManutencao,

    @PositiveOrZero(message = "Tempo de execução não pode ser negativo.")
    @Schema(description = "Tempo de execução em minutos.", example = "90")
    Integer tempoExecucaoMinutos,

    @PositiveOrZero(message = "Custo de mão de obra não pode ser negativo.")
    @Schema(description = "Custo de mão de obra (R$).", example = "150.00")
    Double custoMaoObra,

    @Schema(description = "Peças consumidas (dão baixa no estoque).")
    List<PecaConsumo> pecas
) {
    /** A part to consume on this work order. */
    public record PecaConsumo(Long pecaId, Integer quantidade) {}
}
