package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.service.ManutencaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manutencoes")
public class ManutencaoController {

    private final ManutencaoService manutencaoService;

    public ManutencaoController(ManutencaoService manutencaoService) {
        this.manutencaoService = manutencaoService;
    }

    @PostMapping("/{maquinaId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_GESTOR','ROLE_TECNICO')")
    public ResponseEntity<ManutencaoResponseDTO> cadastrar(
            @PathVariable Long maquinaId,
            @RequestBody @Valid ManutencaoRequestDTO dto
    ) {
        return ResponseEntity.ok(manutencaoService.cadastrar(dto, maquinaId));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String status
    ) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(manutencaoService.listarPaginado(page, size, tipo, status));
    }

    @GetMapping("/maquina/{maquinaId}")
    public ResponseEntity<List<ManutencaoResponseDTO>> listarPorMaquina(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(manutencaoService.listarPorMaquina(maquinaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManutencaoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(manutencaoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_GESTOR','ROLE_TECNICO')")
    public ResponseEntity<ManutencaoResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid ManutencaoRequestDTO dto
    ) {
        return ResponseEntity.ok(manutencaoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_GESTOR')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        manutencaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/preventivas/gerar")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_GESTOR')")
    public ResponseEntity<Map<String, Object>> gerarPreventivas() {
        int geradas = manutencaoService.gerarPreventivasVencidas();
        return ResponseEntity.ok(Map.of(
            "message", "Ordens de serviço preventivas geradas",
            "quantidade", geradas
        ));
    }
}
