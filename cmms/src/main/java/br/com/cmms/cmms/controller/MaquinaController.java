package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.MaquinaRequestDTO;
import br.com.cmms.cmms.dto.MaquinaResponseDTO;
import br.com.cmms.cmms.service.MaquinaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maquinas")
public class MaquinaController {

    private final MaquinaService maquinaService;

    public MaquinaController(MaquinaService maquinaService) {
        this.maquinaService = maquinaService;
    }

    @PostMapping
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

    @PutMapping("/{id}")
    public ResponseEntity<MaquinaResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid MaquinaRequestDTO dto
    ) {
        return ResponseEntity.ok(maquinaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        maquinaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
