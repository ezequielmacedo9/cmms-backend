package br.com.cmms.cmms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload de entrada para criar / atualizar uma ferramenta.
 *
 * <p>Não aceita {@code id} no corpo — impede mass-assignment. O id vem
 * sempre da URL.
 */
@Schema(description = "Dados para cadastro ou atualização de uma ferramenta.")
public record FerramentaRequestDTO(

    @NotBlank(message = "Nome é obrigatório.")
    @Size(min = 2, max = Constraints.NOME_MAX,
        message = "Nome deve ter entre 2 e " + Constraints.NOME_MAX + " caracteres.")
    @Schema(example = "Torquímetro 1/2'' digital")
    String nome,

    @Pattern(regexp = "^$|" + Constraints.CODIGO_REGEX,
        message = "Código deve conter apenas letras, dígitos, '-' ou '_'.")
    @Schema(example = "TQ-1-2-DIG")
    String codigo,

    @Pattern(regexp = "^$|^(DISPONIVEL|EM_USO|MANUTENCAO|EXTRAVIADA)$",
        message = "Status deve ser DISPONIVEL, EM_USO, MANUTENCAO ou EXTRAVIADA.")
    @Schema(example = "DISPONIVEL",
        allowableValues = {"DISPONIVEL", "EM_USO", "MANUTENCAO", "EXTRAVIADA"})
    String status,

    @Size(max = Constraints.LOCALIZACAO_MAX,
        message = "Localização deve ter no máximo " + Constraints.LOCALIZACAO_MAX + " caracteres.")
    @Schema(example = "Almoxarifado - Prateleira A3")
    String localizacao,

    @Size(max = Constraints.NOME_MAX,
        message = "Responsável deve ter no máximo " + Constraints.NOME_MAX + " caracteres.")
    @Schema(example = "Ezequiel Macedo")
    String responsavel,

    @PastOrPresent(message = "Data da última manutenção não pode estar no futuro.")
    @Schema(example = "2026-04-20")
    LocalDate dataUltimaManutencao
) {}
