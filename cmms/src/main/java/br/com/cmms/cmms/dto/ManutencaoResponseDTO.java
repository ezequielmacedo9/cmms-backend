package br.com.cmms.cmms.dto;

import java.time.LocalDate;
import java.util.List;

public record ManutencaoResponseDTO(
    Long id,
    String tipo,
    String tecnico,
    Long tecnicoId,
    String descricao,
    String prioridade,
    String status,
    LocalDate dataManutencao,
    LocalDate dataAbertura,
    LocalDate dataConclusao,
    Integer tempoExecucaoMinutos,
    Double custoMaoObra,
    double custoPecas,
    double custoTotal,
    List<PecaConsumida> pecas,
    List<ChecklistItem> checklist,
    List<Anexo> anexos,
    MaquinaInfo maquina
) {
    public record MaquinaInfo(Long id, String nome, String setor) {}

    public record PecaConsumida(Long pecaId, String pecaNome, int quantidade,
                                double custoUnitario, double subtotal) {}

    public record ChecklistItem(Long id, String descricao, boolean concluido) {}

    /** Attachment metadata (no bytes). Download via GET /api/manutencoes/anexos/{id}. */
    public record Anexo(Long id, String nome, String contentType, int tamanho) {}
}
