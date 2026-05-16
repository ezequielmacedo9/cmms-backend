package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.dto.LoginRequestDTO;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.service.AuthService;
import br.com.cmms.cmms.service.GoogleAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
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
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO request,
                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getSenha(), httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDTO> refresh(@Valid @RequestBody RefreshRequest body) {
        return ResponseEntity.ok(authService.refresh(body.refreshToken()));
    }

    @PostMapping("/google")
    public ResponseEntity<TokenResponseDTO> googleLogin(@Valid @RequestBody GoogleLoginRequest body,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(googleAuthService.loginWithGoogle(body.idToken(), request));
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record GoogleLoginRequest(@NotBlank String idToken) {}
}
