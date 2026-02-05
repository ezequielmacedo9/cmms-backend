package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.PecaRequestDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.service.PecaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pecas")
public class PecaController {

    @Autowired
    private PecaService pecaService;

    @PostMapping
    public ResponseEntity<PecaResponseDTO> cadastrar(@RequestBody PecaRequestDTO dto) {
        return ResponseEntity.ok(pecaService.cadastrar(dto));
    }

    @GetMapping
    public ResponseEntity<List<PecaResponseDTO>> listar() {
        return ResponseEntity.ok(pecaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid PecaRequestDTO dto
    ) {
        return ResponseEntity.ok(pecaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pecaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
