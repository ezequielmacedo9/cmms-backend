package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.ConvidarUsuarioRequestDTO;
import br.com.cmms.cmms.dto.EmpresaResponseDTO;
import br.com.cmms.cmms.dto.UsuarioResponseDTO;
import br.com.cmms.cmms.service.EmpresaService;
import br.com.cmms.cmms.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresa")
public class EmpresaController {

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;

    public EmpresaController(EmpresaService empresaService, UsuarioService usuarioService) {
        this.empresaService = empresaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/minha")
    public ResponseEntity<EmpresaResponseDTO> getMinha() {
        return ResponseEntity.ok(empresaService.getMinha());
    }

    @PutMapping("/minha")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<EmpresaResponseDTO> updateMinha(@RequestBody EmpresaResponseDTO dto) {
        return ResponseEntity.ok(empresaService.updateMinha(dto));
    }

    @GetMapping("/minha/usuarios")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(empresaService.listarUsuarios());
    }

    @PostMapping("/minha/usuarios")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> convidarUsuario(
            @Valid @RequestBody ConvidarUsuarioRequestDTO dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(usuarioService.convidar(dto, email));
    }

    @DeleteMapping("/minha/usuarios/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> removerUsuario(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        empresaService.removerUsuario(id, email);
        return ResponseEntity.noContent().build();
    }
}
