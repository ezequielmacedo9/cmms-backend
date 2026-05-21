package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.AlterarRoleRequestDTO;
import br.com.cmms.cmms.dto.ConvidarUsuarioRequestDTO;
import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.dto.UsuarioResponseDTO;
import br.com.cmms.cmms.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    @Operation(summary = "Obter perfil do usuário autenticado")
    public ResponseEntity<UsuarioResponseDTO> meuPerfil(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.getMeuPerfil(principal.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Listar usuários",
        description = "Restrito a SUPER_ADMIN e ADMIN. unpaged=true devolve lista completa.")
    public ResponseEntity<?> listar(
            @RequestParam(name = "unpaged", defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20) Pageable pageable) {
        if (unpaged) {
            List<UsuarioResponseDTO> all = usuarioService.listar();
            return ResponseEntity.ok(all);
        }
        return ResponseEntity.ok(PagedResponseDTO.of(usuarioService.listar(pageable)));
    }

    @PostMapping("/convidar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Convidar/criar novo usuário",
        description = "ADMIN não pode criar SUPER_ADMIN ou outros ADMINs.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Operador não pode atribuir essa role"),
        @ApiResponse(responseCode = "409", description = "Email já cadastrado")
    })
    public ResponseEntity<UsuarioResponseDTO> convidar(
            @Valid @RequestBody ConvidarUsuarioRequestDTO dto,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.convidar(dto, principal.getUsername()));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Alterar role de um usuário",
        description = "Ninguém pode alterar a própria role. ADMIN não pode promover/rebaixar SUPER_ADMIN/ADMIN.")
    public ResponseEntity<UsuarioResponseDTO> alterarRole(
            @PathVariable Long id,
            @Valid @RequestBody AlterarRoleRequestDTO dto,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.alterarRole(id, dto, principal.getUsername()));
    }

    @PutMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Reativar usuário desativado")
    public ResponseEntity<UsuarioResponseDTO> ativar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.ativar(id, principal.getUsername()));
    }

    @PutMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Desativar usuário",
        description = "Não permite desativar a própria conta.")
    public ResponseEntity<UsuarioResponseDTO> desativar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.desativar(id, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Excluir usuário (soft delete) — apenas SUPER_ADMIN",
        description = "Marca deleted_at; row preserved para auditoria.")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        usuarioService.deletar(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
