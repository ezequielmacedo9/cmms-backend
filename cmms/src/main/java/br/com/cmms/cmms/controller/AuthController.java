package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.security.UserDetailsImpl;
import br.com.cmms.cmms.dto.LoginRequestDTO;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.service.AuthService;
import br.com.cmms.cmms.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin HTTP facade for the authentication flow. All business rules live in
 * the corresponding services. Validation is enforced through Bean Validation
 * on the request DTOs; error translation is centralised in the global
 * exception handler.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autenticar com email e senha",
        description = "Retorna access + refresh tokens. Aplica lockout após múltiplas falhas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login bem-sucedido"),
        @ApiResponse(responseCode = "401", description = "Email ou senha incorretos"),
        @ApiResponse(responseCode = "403", description = "Conta bloqueada por excesso de tentativas")
    })
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO request,
                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getSenha(), httpRequest));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar access token (com rotação do refresh)",
        description = "Recebe um refresh token válido, invalida-o e emite um novo par access+refresh.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens renovados"),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
    })
    public ResponseEntity<TokenResponseDTO> refresh(@Valid @RequestBody RefreshRequest body) {
        return ResponseEntity.ok(authService.refresh(body.refreshToken()));
    }

    @PostMapping("/google")
    @SecurityRequirements
    @Operation(summary = "Login via Google OAuth",
        description = "Valida o id_token do Google e cria/atualiza o usuário local.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login com Google bem-sucedido"),
        @ApiResponse(responseCode = "401", description = "id_token inválido")
    })
    public ResponseEntity<TokenResponseDTO> googleLogin(@Valid @RequestBody GoogleLoginRequest body,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(googleAuthService.loginWithGoogle(body.idToken(), request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão",
        description = "Revoga TODOS os refresh tokens do usuário autenticado. O access token "
                    + "atual continua válido até expirar (≤15min). Idempotente.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logout efetuado"),
        @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetailsImpl principal,
                                       HttpServletRequest request) {
        authService.logout(principal != null ? principal.getUsuario() : null, request);
        return ResponseEntity.noContent().build();
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record GoogleLoginRequest(@NotBlank String idToken) {}
}
