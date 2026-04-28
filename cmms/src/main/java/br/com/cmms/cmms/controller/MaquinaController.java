package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.MaquinaRequestDTO;
import br.com.cmms.cmms.dto.MaquinaResponseDTO;
import br.com.cmms.cmms.service.MaquinaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maquinas")
public class MaquinaController {

    private final MaquinaService maquinaService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public MaquinaController(MaquinaService maquinaService) {
        this.maquinaService = maquinaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_GESTOR')")
    public ResponseEntity<MaquinaResponseDTO> cadastrar(@RequestBody @Valid MaquinaRequestDTO dto) {
        return ResponseEntity.ok(maquinaService.cadastrar(dto));
    }

    @GetMapping
    public ResponseEntity<List<MaquinaResponseDTO>> listar() {
        return ResponseEntity.ok(maquinaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaquinaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(maquinaService.buscarPorId(id));
    }

    @GetMapping("/{id}/qrcode")
    public ResponseEntity<Map<String, String>> qrcode(@PathVariable Long id) {
        MaquinaResponseDTO maquina = maquinaService.buscarPorId(id);
        String targetUrl = frontendUrl + "/maquinas/" + id;
        // Returns QR code as Google Charts API URL (no dependency needed)
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data="
            + java.net.URLEncoder.encode(targetUrl, java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok(Map.of(
            "maquinaId",   String.valueOf(id),
            "maquinaNome", maquina.nome(),
            "targetUrl",   targetUrl,
            "qrImageUrl",  qrUrl
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_GESTOR')")
    public ResponseEntity<MaquinaResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid MaquinaRequestDTO dto
    ) {
        return ResponseEntity.ok(maquinaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        maquinaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
