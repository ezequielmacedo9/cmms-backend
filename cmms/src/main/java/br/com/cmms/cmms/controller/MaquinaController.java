package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.MaquinaRequestDTO;
import br.com.cmms.cmms.dto.MaquinaResponseDTO;
import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.service.MaquinaService;
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
@RequestMapping("/api/maquinas")
public class MaquinaController {

    private final MaquinaService maquinaService;

    public MaquinaController(MaquinaService maquinaService) {
        this.maquinaService = maquinaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    public ResponseEntity<MaquinaResponseDTO> cadastrar(@RequestBody @Valid MaquinaRequestDTO dto) {
        return ResponseEntity.ok(maquinaService.cadastrar(dto));
    }

    /**
     * Paged listing with optional filters.
     * Pass {@code ?unpaged=true} to fall back to the legacy non-paged response
     * (kept for older integrations and report exports).
     */
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20) Pageable pageable) {
        if (unpaged) {
            List<MaquinaResponseDTO> all = maquinaService.listar();
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(PagedResponseDTO.of(maquinaService.listar(q, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaquinaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(maquinaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    public ResponseEntity<MaquinaResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid MaquinaRequestDTO dto
    ) {
        return ResponseEntity.ok(maquinaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        maquinaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
