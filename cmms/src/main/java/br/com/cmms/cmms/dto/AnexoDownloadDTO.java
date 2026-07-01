package br.com.cmms.cmms.dto;

/** Full attachment payload (with base64 bytes) for download. */
public record AnexoDownloadDTO(
    Long id,
    String nome,
    String contentType,
    int tamanho,
    String dadosBase64
) {}
