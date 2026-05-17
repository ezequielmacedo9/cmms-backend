package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
@SecurityRequirements
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar redefinição de senha",
        description = "Sempre retorna 200 para evitar enumeração de e-mails.")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req,
            HttpServletRequest request) {
        passwordResetService.requestReset(req.email(), request);
        // Always 200 — body intentionally generic to prevent e-mail enumeration.
        return ResponseEntity.ok(Map.of("message", "Se o email existir, você receberá as instruções."));
    }

    @GetMapping("/validate-reset-token")
    @Operation(summary = "Validar token de redefinição",
        description = "Verifica se o token de reset ainda está dentro da janela de validade.")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam @NotBlank String token) {
        return ResponseEntity.ok(Map.of("valid", passwordResetService.isTokenValid(token)));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Trocar senha via token de redefinição")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req,
            HttpServletRequest request) {
        passwordResetService.resetPassword(req.token(), req.novaSenha(), request);
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }

    public record ForgotPasswordRequest(
        @Email @NotBlank String email
    ) {}

    public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8) String novaSenha
    ) {}
}
