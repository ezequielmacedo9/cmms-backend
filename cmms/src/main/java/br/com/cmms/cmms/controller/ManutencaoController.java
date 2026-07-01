package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.AnexoDownloadDTO;
import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.service.ManutencaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/manutencoes")
@Tag(name = "Manutenções")
public class ManutencaoController {

    private final ManutencaoService manutencaoService;

    public ManutencaoController(ManutencaoService manutencaoService) {
        this.manutencaoService = manutencaoService;
    }

    @PostMapping("/{maquinaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR','TECNICO')")
    @Operation(summary = "Registrar nova manutenção em uma máquina")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Manutenção registrada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Máquina não encontrada")
    })
    public ResponseEntity<ManutencaoResponseDTO> cadastrar(
            @PathVariable Long maquinaId,
            @RequestBody @Valid ManutencaoRequestDTO dto
    ) {
        return ResponseEntity.ok(manutencaoService.cadastrar(dto, maquinaId));
    }

    @GetMapping
    @Operation(summary = "Listar manutenções",
        description = "Default ordenado por dataManutencao DESC. unpaged=true para lista completa.")
    public ResponseEntity<?> listar(
            @Parameter(description = "Quando true, devolve lista completa.")
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "dataManutencao", direction = Sort.Direction.DESC) Pageable pageable) {
        if (unpaged) {
            List<ManutencaoResponseDTO> all = manutencaoService.listar();
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(PagedResponseDTO.of(manutencaoService.listar(pageable)));
    }

    @GetMapping("/maquina/{maquinaId}")
    @Operation(summary = "Listar manutenções de uma máquina específica")
    public ResponseEntity<?> listarPorMaquina(
            @PathVariable Long maquinaId,
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20) Pageable pageable) {
        if (unpaged) {
            return ResponseEntity.ok(manutencaoService.listarPorMaquina(maquinaId));
        }
        return ResponseEntity.ok(PagedResponseDTO.of(manutencaoService.listarPorMaquina(maquinaId, pageable)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR','TECNICO')")
    @Operation(summary = "Alterar o status da ordem de serviço",
        description = "ABERTA -> EM_ANDAMENTO -> CONCLUIDA (carimba a data de conclusão) / CANCELADA.")
    public ResponseEntity<ManutencaoResponseDTO> alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest body) {
        return ResponseEntity.ok(manutencaoService.alterarStatus(id, body.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    @Operation(summary = "Excluir manutenção (soft delete)",
        description = "A manutenção é marcada como deletada mas permanece no banco para auditoria.")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        manutencaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // ── checklist ──────────────────────────────────────────────────────

    @PostMapping("/{id}/checklist")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR','TECNICO')")
    @Operation(summary = "Adicionar item de checklist à OS")
    public ResponseEntity<ManutencaoResponseDTO> addChecklist(
            @PathVariable Long id, @Valid @RequestBody ChecklistRequest body) {
        return ResponseEntity.ok(manutencaoService.addChecklistItem(id, body.descricao()));
    }

    @PutMapping("/checklist/{itemId}/toggle")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR','TECNICO')")
    @Operation(summary = "Marcar/desmarcar item de checklist")
    public ResponseEntity<ManutencaoResponseDTO> toggleChecklist(@PathVariable Long itemId) {
        return ResponseEntity.ok(manutencaoService.toggleChecklistItem(itemId));
    }

    @DeleteMapping("/checklist/{itemId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    @Operation(summary = "Remover item de checklist")
    public ResponseEntity<ManutencaoResponseDTO> removeChecklist(@PathVariable Long itemId) {
        return ResponseEntity.ok(manutencaoService.removeChecklistItem(itemId));
    }

    // ── anexos ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/anexos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR','TECNICO')")
    @Operation(summary = "Anexar arquivo (base64) à OS")
    public ResponseEntity<ManutencaoResponseDTO> addAnexo(
            @PathVariable Long id, @Valid @RequestBody AnexoRequest body) {
        return ResponseEntity.ok(manutencaoService.addAnexo(id, body.nome(), body.contentType(), body.dadosBase64()));
    }

    @GetMapping("/anexos/{anexoId}")
    @Operation(summary = "Baixar anexo (base64)")
    public ResponseEntity<AnexoDownloadDTO> getAnexo(@PathVariable Long anexoId) {
        return ResponseEntity.ok(manutencaoService.getAnexo(anexoId));
    }

    @DeleteMapping("/anexos/{anexoId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','GESTOR')")
    @Operation(summary = "Remover anexo")
    public ResponseEntity<ManutencaoResponseDTO> removeAnexo(@PathVariable Long anexoId) {
        return ResponseEntity.ok(manutencaoService.removeAnexo(anexoId));
    }

    public record StatusRequest(@NotBlank String status) {}
    public record ChecklistRequest(@NotBlank String descricao) {}
    public record AnexoRequest(@NotBlank String nome, String contentType, @NotBlank String dadosBase64) {}
}
