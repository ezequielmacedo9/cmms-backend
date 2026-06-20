package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.RelatorioGerencialDTO;
import br.com.cmms.cmms.service.RelatorioGerencialService;
import br.com.cmms.cmms.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios")
public class RelatorioController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final RelatorioService service;
    private final RelatorioGerencialService gerencialService;

    public RelatorioController(RelatorioService service, RelatorioGerencialService gerencialService) {
        this.service = service;
        this.gerencialService = gerencialService;
    }

    @GetMapping("/gerencial")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    @Operation(summary = "KPIs gerenciais da empresa",
        description = "Cumprimento de preventiva, MTBF, disponibilidade, valor de estoque e top ofensores.")
    public ResponseEntity<RelatorioGerencialDTO> gerencial() {
        return ResponseEntity.ok(gerencialService.gerar());
    }

    @GetMapping("/manutencoes")
    @Operation(summary = "Exportar relatório de manutenções",
        description = "Gera PDF ou Excel com a lista de manutenções registradas.")
    @ApiResponse(responseCode = "200",
        description = "Arquivo binário (PDF ou XLSX) anexado.")
    public ResponseEntity<byte[]> manutencoes(
            @Parameter(description = "Formato de saída: pdf ou xlsx (padrão).",
                schema = @Schema(allowableValues = {"pdf", "xlsx"}))
            @RequestParam(defaultValue = "xlsx") String format) throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if ("pdf".equalsIgnoreCase(format)) {
            return download(service.manutencoesPdf(), "manutencoes_" + date + ".pdf", "application/pdf");
        }
        return download(service.manutencoesExcel(), "manutencoes_" + date + ".xlsx", XLSX);
    }

    @GetMapping("/maquinas")
    @Operation(summary = "Exportar relatório de máquinas")
    public ResponseEntity<byte[]> maquinas(
            @Parameter(description = "Formato de saída: pdf ou xlsx (padrão).",
                schema = @Schema(allowableValues = {"pdf", "xlsx"}))
            @RequestParam(defaultValue = "xlsx") String format) throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if ("pdf".equalsIgnoreCase(format)) {
            return download(service.maquinasPdf(), "maquinas_" + date + ".pdf", "application/pdf");
        }
        return download(service.maquinasExcel(), "maquinas_" + date + ".xlsx", XLSX);
    }

    @GetMapping("/estoque")
    @Operation(summary = "Exportar relatório de estoque")
    public ResponseEntity<byte[]> estoque(
            @Parameter(description = "Formato de saída: pdf ou xlsx (padrão).",
                schema = @Schema(allowableValues = {"pdf", "xlsx"}))
            @RequestParam(defaultValue = "xlsx") String format) throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if ("pdf".equalsIgnoreCase(format)) {
            return download(service.estoquePdf(), "estoque_" + date + ".pdf", "application/pdf");
        }
        return download(service.estoqueExcel(), "estoque_" + date + ".xlsx", XLSX);
    }

    private ResponseEntity<byte[]> download(byte[] data, String filename, String contentType) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            .body(data);
    }
}
