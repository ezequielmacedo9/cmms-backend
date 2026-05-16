package br.com.cmms.cmms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Input payload for creating or updating a {@code Ferramenta}.
 *
 * <p>Bound exclusively to the API surface — never accepts an {@code id} so
 * mass-assignment attacks (cliente sobrescrevendo IDs alheios) ficam
 * impossíveis. The id always comes from the URL path.
 */
public record FerramentaRequestDTO(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255)
    String nome,

    @Size(max = 255)
    String codigo,

    @Size(max = 20)
    String status,

    @Size(max = 255)
    String localizacao,

    @Size(max = 255)
    String responsavel,

    LocalDate dataUltimaManutencao
) {}
