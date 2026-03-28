package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.Security.JwtService;
import br.com.cmms.cmms.Security.UserDetailsImpl;
import br.com.cmms.cmms.dto.LoginRequestDTO;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.model.RefreshToken;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Login attempt for email: {}", request.getEmail());
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Usuario usuario = userDetails.getUsuario();

        String accessToken = jwtService.gerarToken(usuario);
        String refreshToken = refreshTokenService.criarRefreshToken(usuario).getToken();

        log.info("Login successful for email: {}", request.getEmail());
        return ResponseEntity.ok(new TokenResponseDTO(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        RefreshToken token = refreshTokenService.validar(refreshToken);
        String novoAccessToken = jwtService.gerarToken(token.getUsuario());
        return ResponseEntity.ok(Map.of("accessToken", novoAccessToken));
    }
}
