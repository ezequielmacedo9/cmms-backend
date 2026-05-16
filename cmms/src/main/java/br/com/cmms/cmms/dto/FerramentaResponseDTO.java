package br.com.cmms.cmms.dto;

import br.com.cmms.cmms.model.Ferramenta;

import java.time.LocalDate;

/**
 * Output projection of {@link Ferramenta}. Only the fields the API is
 * willing to expose — no JPA collections or internal flags leak out.
 */
public record FerramentaResponseDTO(
    Long id,
    String nome,
    String codigo,
    String status,
    String localizacao,
    String responsavel,
    LocalDate dataUltimaManutencao
) {

    public static FerramentaResponseDTO from(Ferramenta f) {
        return new FerramentaResponseDTO(
            f.getId(),
            f.getNome(),
            f.getCodigo(),
            f.getStatus(),
            f.getLocalizacao(),
            f.getResponsavel(),
            f.getDataUltimaManutencao()
        );
    }
}
