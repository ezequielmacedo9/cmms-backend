package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.PasswordResetToken;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.PasswordResetTokenRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import br.com.cmms.cmms.service.AuditService;
import br.com.cmms.cmms.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);
    private static final int TOKEN_EXPIRY_HOURS = 1;

    private final UsuarioRepository usuarioRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public PasswordResetController(UsuarioRepository usuarioRepo,
                                   PasswordResetTokenRepository tokenRepo,
                                   EmailService emailService,
                                   PasswordEncoder passwordEncoder,
                                   AuditService audit) {
        this.usuarioRepo = usuarioRepo;
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody ForgotPasswordRequest req,
            HttpServletRequest request) {
        // Always return success to prevent email enumeration
        Optional<Usuario> opt = usuarioRepo.findByEmail(req.email());
        if (opt.isPresent()) {
            Usuario u = opt.get();
            // Revoke old tokens
            tokenRepo.deleteExpiredAndUsed(LocalDateTime.now());

            String rawToken = generateToken();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setToken(rawToken);
            prt.setUsuario(u);
            prt.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
            tokenRepo.save(prt);

            try {
                emailService.sendPasswordReset(u.getEmail(), rawToken, u.getNome());
            } catch (Exception e) {
                log.warn("Failed to send password reset email: {}", e.getMessage());
            }
            audit.log(u.getEmail(), u.getId(), "PASSWORD_RESET_REQUEST", "USUARIO", null, "Solicitação de redefinição", AuditService.getClientIp(request));
        }
        return ResponseEntity.ok(Map.of("message", "Se o email existir, você receberá as instruções."));
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam String token) {
        boolean valid = tokenRepo.findByToken(token)
            .map(t -> !t.isUsado() && !t.isExpired())
            .orElse(false);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest req,
            HttpServletRequest request) {
        PasswordResetToken prt = tokenRepo.findByToken(req.token())
            .orElse(null);
        if (prt == null || prt.isUsado() || prt.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token inválido ou expirado"));
        }
        if (req.novaSenha().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Senha deve ter pelo menos 8 caracteres"));
        }

        Usuario u = prt.getUsuario();
        u.setSenha(passwordEncoder.encode(req.novaSenha()));
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        usuarioRepo.save(u);

        prt.setUsado(true);
        tokenRepo.save(prt);

        audit.log(u.getEmail(), u.getId(), "PASSWORD_RESET", "USUARIO", u.getId(), "Senha redefinida via token", AuditService.getClientIp(request));
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso"));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public record ForgotPasswordRequest(@Email @NotBlank String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8) String novaSenha) {}
}
