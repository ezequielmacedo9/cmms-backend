package br.com.cmms.cmms.dto;

import java.time.LocalDate;

public record MaquinaResponseDTO(
    Long id,
    String nome,
    String setor,
    String status,
    String prioridade,
    Integer intervaloPreventivaDias,
    LocalDate dataUltimaManutencao,
    boolean manutencaoVencida
) {}
