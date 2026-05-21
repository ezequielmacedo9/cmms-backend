package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.dto.PecaRequestDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.service.PecaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Cadastrar nova peça")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Peça criada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<PecaResponseDTO> cadastrar(@Valid @RequestBody PecaRequestDTO dto) {
        return ResponseEntity.ok(pecaService.cadastrar(dto));
    }

    @GetMapping
    @Operation(summary = "Listar peças",
        description = "Retorna Page<PecaResponseDTO> ou List<PecaResponseDTO> quando unpaged=true.")
    public ResponseEntity<?> listar(
            @Parameter(description = "Busca livre por nome ou código.") @RequestParam(required = false) String q,
            @Parameter(description = "Quando true, devolve lista completa sem paginação.")
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20) Pageable pageable) {
        if (unpaged) {
            List<PecaResponseDTO> all = pecaService.listar();
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(PagedResponseDTO.of(pecaService.listar(q, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar peça por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Peça encontrada"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada")
    })
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    @Operation(summary = "Atualizar peça")
    public ResponseEntity<PecaResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid PecaRequestDTO dto
    ) {
        return ResponseEntity.ok(pecaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Excluir peça",
        description = "Remoção física (não há soft delete em peças).")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pecaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
