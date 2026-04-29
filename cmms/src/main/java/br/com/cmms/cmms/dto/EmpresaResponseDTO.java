package br.com.cmms.cmms.dto;

import br.com.cmms.cmms.model.Empresa;
import br.com.cmms.cmms.model.PlanoAssinatura;

import java.time.LocalDateTime;

public record EmpresaResponseDTO(
    Long id,
    String nome,
    String cnpj,
    String email,
    String telefone,
    String endereco,
    PlanoAssinatura plano,
    int limiteAtivos,
    int limiteUsuarios,
    LocalDateTime dataCriacao,
    boolean ativo
) {
    public static EmpresaResponseDTO from(Empresa e) {
        return new EmpresaResponseDTO(
            e.getId(), e.getNome(), e.getCnpj(), e.getEmail(),
            e.getTelefone(), e.getEndereco(), e.getPlano(),
            e.getLimiteAtivos(), e.getLimiteUsuarios(),
            e.getDataCriacao(), e.isAtivo()
        );
    }
}
