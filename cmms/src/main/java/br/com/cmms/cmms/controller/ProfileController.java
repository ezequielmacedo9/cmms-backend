package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.UsuarioRepository;
import br.com.cmms.cmms.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public ProfileController(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder,
                             AuditService audit) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile(@AuthenticationPrincipal UserDetails ud) {
        Usuario u = findUser(ud.getUsername());
        return ResponseEntity.ok(profileMap(u));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest req,
            @AuthenticationPrincipal UserDetails ud,
            HttpServletRequest request) {
        Usuario u = findUser(ud.getUsername());
        u.setNome(req.nome);
        u.setTelefone(req.telefone);
        u.setCargo(req.cargo);
        u.setDepartamento(req.departamento);
        usuarioRepo.save(u);
        audit.log(u.getEmail(), u.getId(), "PROFILE_UPDATE", "USUARIO", u.getId(), "Perfil atualizado", AuditService.getClientIp(request));
        return ResponseEntity.ok(profileMap(u));
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        String base64 = body.get("avatarBase64");
        if (base64 == null || base64.isBlank()) return ResponseEntity.badRequest().build();
        if (base64.length() > 400_000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Imagem muito grande (máx 300KB)"));
        }
        Usuario u = findUser(ud.getUsername());
        u.setAvatarBase64(base64);
        usuarioRepo.save(u);
        return ResponseEntity.ok(Map.of("avatarBase64", base64));
    }

    @PostMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal UserDetails ud,
            HttpServletRequest request) {
        Usuario u = findUser(ud.getUsername());
        if (!passwordEncoder.matches(req.senhaAtual, u.getSenha())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Senha atual incorreta"));
        }
        if (req.novaSenha.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nova senha deve ter pelo menos 8 caracteres"));
        }
        u.setSenha(passwordEncoder.encode(req.novaSenha));
        usuarioRepo.save(u);
        audit.log(u.getEmail(), u.getId(), "PASSWORD_CHANGE", "USUARIO", u.getId(), "Senha alterada", AuditService.getClientIp(request));
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Usuario findUser(String email) {
        return usuarioRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private Map<String, Object> profileMap(Usuario u) {
        // Map.of() supports up to 10 key/value pairs; use Map.ofEntries for 11+.
        return Map.ofEntries(
            Map.entry("id",            u.getId()),
            Map.entry("email",         u.getEmail()),
            Map.entry("nome",          nvl(u.getNome())),
            Map.entry("telefone",      nvl(u.getTelefone())),
            Map.entry("cargo",         nvl(u.getCargo())),
            Map.entry("departamento",  nvl(u.getDepartamento())),
            Map.entry("avatarBase64",  nvl(u.getAvatarBase64())),
            Map.entry("totpEnabled",   u.isTotpEnabled()),
            Map.entry("role",          u.getRole().getNome()),
            Map.entry("dataCriacao",   u.getDataCriacao() != null ? u.getDataCriacao().toString() : ""),
            Map.entry("ultimoLogin",   u.getUltimoLogin() != null ? u.getUltimoLogin().toString() : "")
        );
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ── inner DTOs ────────────────────────────────────────────────────────

    public record ProfileUpdateRequest(
        String nome,
        String telefone,
        String cargo,
        String departamento
    ) {}

    public record ChangePasswordRequest(
        @NotBlank String senhaAtual,
        @NotBlank @Size(min = 8) String novaSenha
    ) {}
}
