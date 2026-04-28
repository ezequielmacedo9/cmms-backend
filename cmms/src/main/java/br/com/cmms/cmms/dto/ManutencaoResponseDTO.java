package br.com.cmms.cmms.dto;

import java.time.LocalDate;

public record ManutencaoResponseDTO(
    Long id,
    String tipo,
    String tecnico,
    String descricao,
    String prioridade,
    String status,
    LocalDate dataManutencao,
    LocalDate prazoSla,
    LocalDate dataConclusao,
    Double horasParada,
    Double custoMaoDeObra,
    Double custoTotal,
    String observacoesTecnico,
    boolean slaVencido,
    MaquinaInfo maquina
) {
    public record MaquinaInfo(Long id, String nome, String setor) {}
}
