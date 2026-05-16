package br.com.cmms.cmms.dto;

import br.com.cmms.cmms.model.ConfiguracaoSistema;

/**
 * Read/write representation of a single system-configuration entry.
 *
 * <p>The {@code descricao} and {@code tipo} fields are derived from the seed
 * catalog (see {@code ConfiguracaoService.seed()}) — the API never lets the
 * client invent new keys or change types, only update values of existing
 * entries.
 */
public record ConfiguracaoSistemaDTO(
    String chave,
    String valor,
    String grupo,
    String tipo,
    String descricao
) {

    public static ConfiguracaoSistemaDTO from(ConfiguracaoSistema c) {
        return new ConfiguracaoSistemaDTO(
            c.getChave(),
            c.getValor(),
            c.getGrupo(),
            c.getTipo(),
            c.getDescricao()
        );
    }
}
