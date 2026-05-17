package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.AlterarRoleRequestDTO;
import br.com.cmms.cmms.dto.ConvidarUsuarioRequestDTO;
import br.com.cmms.cmms.dto.PagedResponseDTO;
import br.com.cmms.cmms.dto.UsuarioResponseDTO;
import br.com.cmms.cmms.service.UsuarioService;
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
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> meuPerfil(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.getMeuPerfil(principal.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
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
    public ResponseEntity<UsuarioResponseDTO> convidar(
            @Valid @RequestBody ConvidarUsuarioRequestDTO dto,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.convidar(dto, principal.getUsername()));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> alterarRole(
            @PathVariable Long id,
            @Valid @RequestBody AlterarRoleRequestDTO dto,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.alterarRole(id, dto, principal.getUsername()));
    }

    @PutMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> ativar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.ativar(id, principal.getUsername()));
    }

    @PutMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> desativar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(usuarioService.desativar(id, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        usuarioService.deletar(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
