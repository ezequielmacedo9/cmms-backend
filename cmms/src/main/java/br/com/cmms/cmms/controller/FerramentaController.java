package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.FerramentaRequestDTO;
import br.com.cmms.cmms.dto.FerramentaResponseDTO;
import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.service.FerramentaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ferramentas")
public class FerramentaController {

    private final FerramentaService ferramentaService;

    public FerramentaController(FerramentaService ferramentaService) {
        this.ferramentaService = ferramentaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    public ResponseEntity<FerramentaResponseDTO> cadastrar(@Valid @RequestBody FerramentaRequestDTO dto) {
        return ResponseEntity.ok(ferramentaService.cadastrar(dto));
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String q,
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20) Pageable pageable) {
        if (unpaged) {
            List<FerramentaResponseDTO> all = ferramentaService.listar();
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(PagedResponseDTO.of(ferramentaService.listar(q, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FerramentaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ferramentaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    public ResponseEntity<FerramentaResponseDTO> atualizar(@PathVariable Long id,
                                                           @Valid @RequestBody FerramentaRequestDTO dto) {
        return ResponseEntity.ok(ferramentaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        ferramentaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
