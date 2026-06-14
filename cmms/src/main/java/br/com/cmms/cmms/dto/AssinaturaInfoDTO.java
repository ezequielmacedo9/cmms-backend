package br.com.cmms.cmms.dto;

import br.com.cmms.cmms.model.Assinatura;

/**
 * Subscription snapshot returned to the billing UI. Dates are ISO strings so
 * the frontend can build a {@code Date} directly.
 */
public record AssinaturaInfoDTO(
    Long id,
    String plano,
    double valorMensal,
    String status,
    String dataInicio,
    String dataProximaCobranca,
    int diasTrial,
    boolean trialAtivo,
    boolean acesso
) {
    public static AssinaturaInfoDTO from(Assinatura a) {
        return new AssinaturaInfoDTO(
            a.getId(),
            a.getPlano(),
            a.getPlanoEnum().getValorMensal(),
            a.getStatus(),
            a.getDataInicio() != null ? a.getDataInicio().toString() : null,
            a.getDataProximaCobranca() != null ? a.getDataProximaCobranca().toString() : null,
            a.diasTrialRestantes(),
            a.trialAtivo(),
            a.temAcesso()
        );
    }
}
