package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.Security.JwtService;
import br.com.cmms.cmms.Security.UserDetailsImpl;
import br.com.cmms.cmms.dto.LoginRequestDTO;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.model.RefreshToken;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import br.com.cmms.cmms.service.AuditService;
import br.com.cmms.cmms.service.ConfiguracaoService;
import br.com.cmms.cmms.service.RefreshTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String GOOGLE_TOKEN_INFO = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioRepository usuarioRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguracaoService config;
    private final AuditService audit;
    private final ObjectMapper objectMapper;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          UsuarioRepository usuarioRepo,
                          RoleRepository roleRepo,
                          PasswordEncoder passwordEncoder,
                          ConfiguracaoService config,
                          AuditService audit,
                          ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.usuarioRepo = usuarioRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.config = config;
        this.audit = audit;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request,
                                   HttpServletRequest httpRequest) {
        log.info("Login attempt: {}", request.getEmail());
        Optional<Usuario> opt = usuarioRepo.findByEmail(request.getEmail());

        if (opt.isPresent()) {
            Usuario u = opt.get();
            // Check lock
            if (!u.isAccountNonLocked()) {
                return ResponseEntity.status(423).body(Map.of("error",
                    "Conta bloqueada. Tente novamente em alguns minutos."));
            }
            try {
                Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
                );
                UserDetailsImpl ud = (UserDetailsImpl) auth.getPrincipal();
                u = ud.getUsuario();

                // Reset failed attempts on success
                u.setFailedLoginAttempts(0);
                u.setLockedUntil(null);
                u.setUltimoLogin(LocalDateTime.now());
                usuarioRepo.save(u);

                String accessToken  = jwtService.gerarToken(u);
                String refreshToken = refreshTokenService.criarRefreshToken(u).getToken();
                audit.log(u.getEmail(), u.getId(), "LOGIN", "AUTH", null, "Login bem-sucedido", AuditService.getClientIp(httpRequest));

                return ResponseEntity.ok(new TokenResponseDTO(
                    accessToken, refreshToken,
                    u.getRole().getNome(),
                    u.getNome() != null ? u.getNome() : u.getEmail(),
                    u.getId()
                ));
            } catch (BadCredentialsException e) {
                // Increment failed attempts
                int attempts = u.getFailedLoginAttempts() + 1;
                int maxAttempts = config.getInt("seguranca.lockout.tentativas", 5);
                u.setFailedLoginAttempts(attempts);
                if (attempts >= maxAttempts) {
                    int lockMinutes = config.getInt("seguranca.lockout.minutos", 15);
                    u.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                    log.warn("Account locked: {} after {} attempts", request.getEmail(), attempts);
                }
                usuarioRepo.save(u);
                audit.log(u.getEmail(), u.getId(), "LOGIN_FAILED", "AUTH", null, "Tentativa " + attempts, AuditService.getClientIp(httpRequest));
                return ResponseEntity.status(401).body(Map.of("error", "Email ou senha incorretos"));
            }
        }

        return ResponseEntity.status(401).body(Map.of("error", "Email ou senha incorretos"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody(required = false) Map<String, String> body) {
        if (body == null || !body.containsKey("refreshToken")) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken é obrigatório"));
        }
        RefreshToken token = refreshTokenService.validar(body.get("refreshToken"));
        String novoAccessToken = jwtService.gerarToken(token.getUsuario());
        return ResponseEntity.ok(Map.of("accessToken", novoAccessToken));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body,
                                          HttpServletRequest request) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "idToken é obrigatório"));
        }

        try {
            RestTemplate rt = new RestTemplate();
            String json = rt.getForObject(GOOGLE_TOKEN_INFO + idToken, String.class);
            JsonNode node = objectMapper.readTree(json);

            if (node.has("error_description")) {
                return ResponseEntity.status(401).body(Map.of("error", "Token Google inválido"));
            }

            String googleEmail = node.path("email").asText();
            String googleId    = node.path("sub").asText();
            String googleNome  = node.path("name").asText();

            Optional<Usuario> opt = usuarioRepo.findByEmail(googleEmail);
            Usuario u;
            if (opt.isPresent()) {
                u = opt.get();
                if (u.getGoogleId() == null) { u.setGoogleId(googleId); usuarioRepo.save(u); }
            } else {
                // Auto-create with VISUALIZADOR role
                Role role = roleRepo.findByNome("ROLE_VISUALIZADOR")
                    .orElseThrow(() -> new RuntimeException("Role não encontrada"));
                u = new Usuario();
                u.setEmail(googleEmail);
                u.setNome(googleNome);
                u.setGoogleId(googleId);
                u.setSenha(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                u.setRole(role);
                u = usuarioRepo.save(u);
            }

            u.setUltimoLogin(LocalDateTime.now());
            usuarioRepo.save(u);
            audit.log(u.getEmail(), u.getId(), "LOGIN_GOOGLE", "AUTH", null, "Login via Google", AuditService.getClientIp(request));

            String accessToken  = jwtService.gerarToken(u);
            String refreshToken = refreshTokenService.criarRefreshToken(u).getToken();
            return ResponseEntity.ok(new TokenResponseDTO(
                accessToken, refreshToken,
                u.getRole().getNome(),
                u.getNome() != null ? u.getNome() : u.getEmail(),
                u.getId()
            ));
        } catch (Exception e) {
            log.error("Google auth error: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Falha na autenticação Google"));
        }
    }
}
