package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.FerramentaRequestDTO;
import br.com.cmms.cmms.dto.FerramentaResponseDTO;
import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.service.FerramentaService;
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
@RequestMapping("/api/ferramentas")
@Tag(name = "Ferramentas")
public class FerramentaController {

    private final FerramentaService ferramentaService;

    public FerramentaController(FerramentaService ferramentaService) {
        this.ferramentaService = ferramentaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    @Operation(summary = "Cadastrar nova ferramenta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ferramenta criada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<FerramentaResponseDTO> cadastrar(@Valid @RequestBody FerramentaRequestDTO dto) {
        return ResponseEntity.ok(ferramentaService.cadastrar(dto));
    }

    @GetMapping
    @Operation(summary = "Listar ferramentas",
        description = "Retorna Page<FerramentaResponseDTO> ou List quando unpaged=true.")
    public ResponseEntity<?> listar(
            @Parameter(description = "Busca livre por nome ou código.") @RequestParam(required = false) String q,
            @Parameter(description = "Quando true, devolve lista completa sem paginação.")
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20) Pageable pageable) {
        if (unpaged) {
            List<FerramentaResponseDTO> all = ferramentaService.listar();
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(PagedResponseDTO.of(ferramentaService.listar(q, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ferramenta por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ferramenta encontrada"),
        @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada")
    })
    public ResponseEntity<FerramentaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ferramentaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    @Operation(summary = "Atualizar ferramenta")
    public ResponseEntity<FerramentaResponseDTO> atualizar(@PathVariable Long id,
                                                           @Valid @RequestBody FerramentaRequestDTO dto) {
        return ResponseEntity.ok(ferramentaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Excluir ferramenta")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        ferramentaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
