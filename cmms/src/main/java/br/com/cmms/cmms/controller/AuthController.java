package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.Security.JwtService;
import br.com.cmms.cmms.dto.LoginRequestDTO;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.model.RefreshToken;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

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
    public ResponseEntity<TokenResponseDTO> login(
            @RequestBody LoginRequestDTO request
    ) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getSenha()
                )
        );

        Usuario usuario = (Usuario) auth.getPrincipal();

        String accessToken = jwtService.gerarToken(usuario);
        String refreshToken =
                refreshTokenService.criarRefreshToken(usuario).getToken();

        return ResponseEntity.ok(
                new TokenResponseDTO(accessToken, refreshToken)
        );
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody Map<String, String> body
    ) {
        String refreshToken = body.get("refreshToken");

        RefreshToken token =
                refreshTokenService.validar(refreshToken);

        String novoAccessToken =
                jwtService.gerarToken(token.getUsuario());

        return ResponseEntity.ok(
                Map.of("accessToken", novoAccessToken)
        );
    }
}
