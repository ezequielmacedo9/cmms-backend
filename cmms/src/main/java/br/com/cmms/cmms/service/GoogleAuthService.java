package br.com.cmms.cmms.service;

import br.com.cmms.cmms.security.JwtService;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.exception.UnauthorizedException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.Empresa;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.EmpresaRepository;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Verifies Google ID tokens and provisions / refreshes the matching local
 * {@link Usuario}. New accounts are created with the {@code ROLE_VISUALIZADOR}
 * role so that random Google sign-ins never start with elevated privileges.
 */
@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);
    private static final String GOOGLE_TOKEN_INFO = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    /** A brand-new Google sign-in creates its own empresa and becomes its admin. */
    private static final String NEW_USER_ROLE = "ROLE_ADMIN";

    private final UsuarioRepository usuarioRepo;
    private final RoleRepository roleRepo;
    private final EmpresaRepository empresaRepo;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService audit;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    public GoogleAuthService(UsuarioRepository usuarioRepo,
                             RoleRepository roleRepo,
                             EmpresaRepository empresaRepo,
                             JwtService jwtService,
                             RefreshTokenService refreshTokenService,
                             AuditService audit,
                             RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.roleRepo = roleRepo;
        this.empresaRepo = empresaRepo;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.audit = audit;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validates the supplied Google ID token, finds or creates the local user,
     * and returns issued access/refresh tokens.
     *
     * @throws ValidationException   if the {@code idToken} is missing/blank
     * @throws UnauthorizedException if Google rejects the token or it cannot be parsed
     */
    @Transactional
    public TokenResponseDTO loginWithGoogle(String idToken, HttpServletRequest request) {
        if (idToken == null || idToken.isBlank()) {
            throw new ValidationException("GOOGLE_TOKEN_MISSING", "idToken é obrigatório.");
        }

        JsonNode payload;
        try {
            String json = restTemplate.getForObject(GOOGLE_TOKEN_INFO + idToken, String.class);
            payload = objectMapper.readTree(json);
        } catch (Exception e) {
            // RestClientException, IOException, parsing errors — all funnel to the same response.
            log.warn("Google tokeninfo lookup failed: {}", e.getMessage());
            throw new UnauthorizedException("GOOGLE_TOKEN_INVALID", "Token Google inválido.");
        }

        if (payload == null || payload.has("error_description")) {
            throw new UnauthorizedException("GOOGLE_TOKEN_INVALID", "Token Google inválido.");
        }

        String googleEmail = payload.path("email").asText();
        String googleId    = payload.path("sub").asText();
        String googleNome  = payload.path("name").asText();

        if (googleEmail.isBlank() || googleId.isBlank()) {
            throw new UnauthorizedException("GOOGLE_TOKEN_INVALID", "Token Google sem email ou sub.");
        }

        Usuario u = findOrProvision(googleEmail, googleId, googleNome);
        u.setUltimoLogin(LocalDateTime.now());
        usuarioRepo.save(u);

        audit.log(u.getEmpresaId(), u.getEmail(), u.getId(), "LOGIN_GOOGLE", "AUTH", null,
            "Login via Google", AuditService.getClientIp(request));

        String accessToken  = jwtService.gerarToken(u);
        String refreshToken = refreshTokenService.criarRefreshToken(u).getToken();
        return new TokenResponseDTO(
            accessToken, refreshToken,
            u.getRole().getNome(),
            u.getNome() != null ? u.getNome() : u.getEmail(),
            u.getId()
        );
    }

    private Usuario findOrProvision(String googleEmail, String googleId, String googleNome) {
        Optional<Usuario> opt = usuarioRepo.findByEmail(googleEmail);
        if (opt.isPresent()) {
            Usuario existing = opt.get();
            if (existing.getGoogleId() == null) {
                existing.setGoogleId(googleId);
                usuarioRepo.save(existing);
            }
            return existing;
        }
        Role role = roleRepo.findByNome(NEW_USER_ROLE)
            .orElseThrow(() -> new IllegalStateException("Required role missing: " + NEW_USER_ROLE));
        // A new Google sign-in bootstraps its own tenant so the user has an
        // isolated workspace from the first request.
        String nomeEmpresa = (googleNome != null && !googleNome.isBlank())
            ? "Empresa de " + googleNome : "Minha Empresa";
        Empresa empresa = empresaRepo.save(new Empresa(nomeEmpresa));

        Usuario novo = new Usuario();
        novo.setEmail(googleEmail);
        novo.setNome(googleNome);
        novo.setGoogleId(googleId);
        // Random unguessable bcrypt hash — user never logs in with email/password here.
        novo.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
        novo.setRole(role);
        novo.setEmpresaId(empresa.getId());
        return usuarioRepo.save(novo);
    }
}
