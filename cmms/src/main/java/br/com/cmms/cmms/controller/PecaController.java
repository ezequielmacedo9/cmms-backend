package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.dto.PecaRequestDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.service.PecaService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/pecas")
@Tag(name = "Peças")
public class PecaController {

    private final PecaService pecaService;

    public PecaController(PecaService pecaService) {
        this.pecaService = pecaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    public ResponseEntity<PecaResponseDTO> cadastrar(@Valid @RequestBody PecaRequestDTO dto) {
        return ResponseEntity.ok(pecaService.cadastrar(dto));
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String q,
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20) Pageable pageable) {
        if (unpaged) {
            List<PecaResponseDTO> all = pecaService.listar();
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(PagedResponseDTO.of(pecaService.listar(q, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    public ResponseEntity<PecaResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid PecaRequestDTO dto
    ) {
        return ResponseEntity.ok(pecaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pecaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
