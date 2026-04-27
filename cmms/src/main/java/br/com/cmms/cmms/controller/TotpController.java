package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.UsuarioRepository;
import br.com.cmms.cmms.service.AuditService;
import br.com.cmms.cmms.service.TotpService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/profile/2fa")
public class TotpController {

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

    /** Step 1: generate secret + QR code data URI */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@AuthenticationPrincipal UserDetails ud) {
        Usuario u = findUser(ud.getUsername());
        String secret = totpService.generateSecret();
        String uri = totpService.buildOtpAuthUri("CMMS", u.getEmail(), secret);

        // temporarily store in session — store in user field with a "pending" prefix
        u.setTotpSecret("PENDING:" + secret);
        u.setTotpEnabled(false);
        usuarioRepo.save(u);

        // Generate QR code as base64 data URI
        String qrDataUri = generateQrDataUri(uri);
        return ResponseEntity.ok(Map.of(
            "secret", secret,
            "qrCodeDataUri", qrDataUri,
            "manualEntryKey", secret
        ));
    }

    /** Step 2: verify TOTP code to confirm setup and enable 2FA */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud,
            HttpServletRequest request) {
        String code = body.get("code");
        Usuario u = findUser(ud.getUsername());
        String storedSecret = u.getTotpSecret();

        if (storedSecret == null || !storedSecret.startsWith("PENDING:")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Setup não iniciado. Chame /setup primeiro."));
        }

        String realSecret = storedSecret.substring(8); // strip "PENDING:"
        if (!totpService.verifyCode(realSecret, code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código inválido"));
        }

        u.setTotpSecret(realSecret);
        u.setTotpEnabled(true);
        usuarioRepo.save(u);
        audit.log(u.getEmail(), u.getId(), "2FA_ENABLED", "USUARIO", u.getId(), "2FA ativado", AuditService.getClientIp(request));
        return ResponseEntity.ok(Map.of("message", "2FA ativado com sucesso"));
    }

    /** Disable 2FA (requires current password confirmation) */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> disable(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud,
            HttpServletRequest request) {
        String senha = body.get("senha");
        Usuario u = findUser(ud.getUsername());
        if (senha == null || !passwordEncoder.matches(senha, u.getSenha())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Senha incorreta"));
        }
        u.setTotpSecret(null);
        u.setTotpEnabled(false);
        usuarioRepo.save(u);
        audit.log(u.getEmail(), u.getId(), "2FA_DISABLED", "USUARIO", u.getId(), "2FA desativado", AuditService.getClientIp(request));
        return ResponseEntity.ok(Map.of("message", "2FA desativado"));
    }

    /** Verify 2FA code during login (call after password auth) */
    @PostMapping("/login-verify")
    public ResponseEntity<Map<String, Boolean>> loginVerify(@RequestBody Map<String, String> body,
                                                             @AuthenticationPrincipal UserDetails ud) {
        String code = body.get("code");
        Usuario u = findUser(ud.getUsername());
        if (!u.isTotpEnabled()) return ResponseEntity.ok(Map.of("valid", true));
        return ResponseEntity.ok(Map.of("valid", totpService.verifyCode(u.getTotpSecret(), code)));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Usuario findUser(String email) {
        return usuarioRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private String generateQrDataUri(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            var matrix = writer.encode(content, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", bos);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("QR code generation failed", e);
        }
    }
}
