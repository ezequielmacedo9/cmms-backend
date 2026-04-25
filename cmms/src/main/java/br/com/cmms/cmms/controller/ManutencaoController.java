package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.service.ManutencaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manutencoes")
public class ManutencaoController {

    private final ManutencaoService manutencaoService;

    public ManutencaoController(ManutencaoService manutencaoService) {
        this.manutencaoService = manutencaoService;
    }

    @PostMapping("/{maquinaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR','TECNICO')")
    public ResponseEntity<ManutencaoResponseDTO> cadastrar(
            @PathVariable Long maquinaId,
            @RequestBody @Valid ManutencaoRequestDTO dto
    ) {
        return ResponseEntity.ok(manutencaoService.cadastrar(dto, maquinaId));
    }

    @GetMapping
    public ResponseEntity<List<ManutencaoResponseDTO>> listar() {
        return ResponseEntity.ok(manutencaoService.listar());
    }

    @GetMapping("/maquina/{maquinaId}")
    public ResponseEntity<List<ManutencaoResponseDTO>> listarPorMaquina(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(manutencaoService.listarPorMaquina(maquinaId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        manutencaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
