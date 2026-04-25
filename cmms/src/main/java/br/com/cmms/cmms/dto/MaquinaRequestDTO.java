package br.com.cmms.cmms.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record MaquinaRequestDTO(
    @NotBlank(message = "Nome é obrigatório") String nome,
    @NotBlank(message = "Setor é obrigatório") String setor,
    @NotBlank(message = "Status é obrigatório") String status,
    String prioridade,
    Integer intervaloPreventivaDias,
    LocalDate dataUltimaManutencao
) {}
