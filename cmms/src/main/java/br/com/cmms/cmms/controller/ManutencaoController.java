package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.service.ManutencaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manutencoes")
@Tag(name = "Manutenções")
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
    public ResponseEntity<?> listar(
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "dataManutencao", direction = Sort.Direction.DESC) Pageable pageable) {
        if (unpaged) {
            List<ManutencaoResponseDTO> all = manutencaoService.listar();
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(PagedResponseDTO.of(manutencaoService.listar(pageable)));
    }

    @GetMapping("/maquina/{maquinaId}")
    public ResponseEntity<?> listarPorMaquina(
            @PathVariable Long maquinaId,
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20) Pageable pageable) {
        if (unpaged) {
            return ResponseEntity.ok(manutencaoService.listarPorMaquina(maquinaId));
        }
        return ResponseEntity.ok(PagedResponseDTO.of(manutencaoService.listarPorMaquina(maquinaId, pageable)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        manutencaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
