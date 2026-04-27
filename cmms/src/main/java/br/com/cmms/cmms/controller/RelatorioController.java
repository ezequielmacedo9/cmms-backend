package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.service.RelatorioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) { this.service = service; }

    @GetMapping("/manutencoes")
    public ResponseEntity<byte[]> manutencoes(@RequestParam(defaultValue = "xlsx") String format) throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if ("pdf".equalsIgnoreCase(format)) {
            return download(service.manutencoesPdf(), "manutencoes_" + date + ".pdf", "application/pdf");
        }
        return download(service.manutencoesExcel(), "manutencoes_" + date + ".xlsx", XLSX);
    }

    @GetMapping("/maquinas")
    public ResponseEntity<byte[]> maquinas(@RequestParam(defaultValue = "xlsx") String format) throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if ("pdf".equalsIgnoreCase(format)) {
            return download(service.maquinasPdf(), "maquinas_" + date + ".pdf", "application/pdf");
        }
        return download(service.maquinasExcel(), "maquinas_" + date + ".xlsx", XLSX);
    }

    @GetMapping("/estoque")
    public ResponseEntity<byte[]> estoque(@RequestParam(defaultValue = "xlsx") String format) throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if ("pdf".equalsIgnoreCase(format)) {
            return download(service.estoquePdf(), "estoque_" + date + ".pdf", "application/pdf");
        }
        return download(service.estoqueExcel(), "estoque_" + date + ".xlsx", XLSX);
    }

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ResponseEntity<byte[]> download(byte[] data, String filename, String contentType) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            .body(data);
    }
}
