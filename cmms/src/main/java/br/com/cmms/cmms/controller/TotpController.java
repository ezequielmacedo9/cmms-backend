package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.exception.UnauthorizedException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.UsuarioRepository;
import br.com.cmms.cmms.service.AuditService;
import br.com.cmms.cmms.service.TotpService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/profile/2fa")
public class TotpController {

    private static final String PENDING_PREFIX = "PENDING:";

    private final TotpService totpService;
    private final UsuarioRepository usuarioRepo;
    private final AuditService audit;
    private final PasswordEncoder passwordEncoder;

    public TotpController(TotpService totpService, UsuarioRepository usuarioRepo,
                          AuditService audit, PasswordEncoder passwordEncoder) {
        this.totpService = totpService;
        this.usuarioRepo = usuarioRepo;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
    }

    /** Step 1: generate secret + QR code data URI. */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@AuthenticationPrincipal UserDetails ud) {
        Usuario u = findUser(ud.getUsername());
        String secret = totpService.generateSecret();
        String uri = totpService.buildOtpAuthUri("CMMS", u.getEmail(), secret);

        // Mark as pending until the user proves possession with /verify.
        u.setTotpSecret(PENDING_PREFIX + secret);
        u.setTotpEnabled(false);
        usuarioRepo.save(u);

        return ResponseEntity.ok(Map.of(
            "secret",         secret,
            "qrCodeDataUri",  generateQrDataUri(uri),
            "manualEntryKey", secret
        ));
    }

    /** Step 2: verify TOTP code to confirm setup and enable 2FA. */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @Valid @RequestBody CodeRequest body,
            @AuthenticationPrincipal UserDetails ud,
            HttpServletRequest request) {
        Usuario u = findUser(ud.getUsername());
        String stored = u.getTotpSecret();

        if (stored == null || !stored.startsWith(PENDING_PREFIX)) {
            throw new ValidationException("TOTP_SETUP_NOT_STARTED",
                "Setup não iniciado. Chame /setup primeiro.");
        }
        String secret = stored.substring(PENDING_PREFIX.length());
        if (!totpService.verifyCode(secret, body.code())) {
            throw new ValidationException("TOTP_CODE_INVALID", "Código inválido.");
        }

        u.setTotpSecret(secret);
        u.setTotpEnabled(true);
        usuarioRepo.save(u);
        audit.log(u.getEmail(), u.getId(), "2FA_ENABLED", "USUARIO", u.getId(),
            "2FA ativado", AuditService.getClientIp(request));
        return ResponseEntity.ok(Map.of("message", "2FA ativado com sucesso."));
    }

    /** Disable 2FA (requires current password confirmation). */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> disable(
            @Valid @RequestBody PasswordRequest body,
            @AuthenticationPrincipal UserDetails ud,
            HttpServletRequest request) {
        Usuario u = findUser(ud.getUsername());
        if (!passwordEncoder.matches(body.senha(), u.getSenha())) {
            throw new UnauthorizedException("PASSWORD_INCORRECT", "Senha incorreta.");
        }
        u.setTotpSecret(null);
        u.setTotpEnabled(false);
        usuarioRepo.save(u);
        audit.log(u.getEmail(), u.getId(), "2FA_DISABLED", "USUARIO", u.getId(),
            "2FA desativado", AuditService.getClientIp(request));
        return ResponseEntity.ok(Map.of("message", "2FA desativado."));
    }

    /** Verify 2FA code during login (call after password auth). */
    @PostMapping("/login-verify")
    public ResponseEntity<Map<String, Boolean>> loginVerify(@Valid @RequestBody CodeRequest body,
                                                             @AuthenticationPrincipal UserDetails ud) {
        Usuario u = findUser(ud.getUsername());
        if (!u.isTotpEnabled()) return ResponseEntity.ok(Map.of("valid", true));
        return ResponseEntity.ok(Map.of("valid", totpService.verifyCode(u.getTotpSecret(), body.code())));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Usuario findUser(String email) {
        return usuarioRepo.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("USUARIO_NOT_FOUND", "Usuário não encontrado."));
    }

    private String generateQrDataUri(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            var matrix = writer.encode(content, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", bos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("QR code generation failed", e);
        }
    }

    public record CodeRequest(@NotBlank @Size(min = 6, max = 8) String code) {}
    public record PasswordRequest(@NotBlank @Size(min = 1, max = 100) String senha) {}
}
