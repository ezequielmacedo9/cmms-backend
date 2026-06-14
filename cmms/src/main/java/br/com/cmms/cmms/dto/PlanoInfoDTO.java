package br.com.cmms.cmms.dto;

import br.com.cmms.cmms.model.Plano;

/**
 * One plan card for the pricing grid. {@code limiteAtivos}/{@code limiteUsuarios}
 * are {@code Object} because the UI shows either a number or "Ilimitado".
 */
public record PlanoInfoDTO(
    String nome,
    Object limiteAtivos,
    Object limiteUsuarios,
    double valorMensal
) {
    public static PlanoInfoDTO from(Plano p) {
        return new PlanoInfoDTO(
            p.name(),
            p.limiteAtivosLabel(),
            p.limiteUsuariosLabel(),
            p.getValorMensal()
        );
    }
}
