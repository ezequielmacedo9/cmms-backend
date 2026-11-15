package br.com.cmms.cmms.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Verifies Google ID tokens and logs in (or provisions) the matching local
 * {@link Usuario}.
 *
 * <p>Google signs a valid ID token for whichever app the user signed into,
 * so a valid signature alone proves nothing for this API. The token is only
 * accepted when its {@code aud} is this app's client id, its issuer is
 * Google and its e-mail is verified. Existing accounts also go through the
 * same lockout and 2FA rules as the password login.
 */
@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);
    private static final String GOOGLE_TOKEN_INFO = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    /** A brand-new Google sign-in creates its own empresa and becomes its admin. */
    private static final String NEW_USER_ROLE = "ROLE_ADMIN";

    private final UsuarioRepository usuarioRepo;
    private final RoleRepository roleRepo;
    private final EmpresaRepository empresaRepo;
    private final AuthService authService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final String googleClientId;

    public GoogleAuthService(UsuarioRepository usuarioRepo,
                             RoleRepository roleRepo,
                             EmpresaRepository empresaRepo,
                             AuthService authService,
                             RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             PasswordEncoder passwordEncoder,
                             @Value("${google.client.id:}") String googleClientId) {
        this.usuarioRepo = usuarioRepo;
        this.roleRepo = roleRepo;
        this.empresaRepo = empresaRepo;
        this.authService = authService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.googleClientId = googleClientId;
    }

    /** Claims taken from a token that passed every check. */
    private record GoogleIdentity(String email, String sub, String nome) {}

    /**
     * Validates the Google ID token, applies lockout and 2FA to existing
     * accounts (or provisions a new empresa + admin) and issues tokens.
     *
     * @throws ValidationException   if the {@code idToken} is missing/blank
     * @throws UnauthorizedException if Google login is not configured, the token is
     *                               rejected, or the second factor is missing/wrong
     */
    // noRollbackFor: a wrong 2FA code must keep the failed-attempt counter.
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponseDTO loginWithGoogle(String idToken, String totpCode, HttpServletRequest request) {
        if (googleClientId == null || googleClientId.isBlank()) {
            // Fail closed: without the client id the audience cannot be checked.
            throw new UnauthorizedException("GOOGLE_LOGIN_DISABLED", "Login com Google não está configurado.");
        }
        if (idToken == null || idToken.isBlank()) {
            throw new ValidationException("GOOGLE_TOKEN_MISSING", "idToken é obrigatório.");
        }

        GoogleIdentity identity = verificarToken(idToken);
        Optional<Usuario> existente = usuarioRepo.findByEmail(identity.email());
        if (existente.isEmpty()) {
            return authService.concluirLogin(provisionar(identity), "LOGIN_GOOGLE", "Conta criada via Google", request);
        }

        Usuario u = existente.get();
        validarContaExistente(u, identity.sub());
        authService.verificarSegundoFator(u, totpCode, request);
        if (u.getGoogleId() == null) {
            // Link only after every check passed.
            u.setGoogleId(identity.sub());
        }
        return authService.concluirLogin(u, "LOGIN_GOOGLE", "Login via Google", request);
    }

    private GoogleIdentity verificarToken(String idToken) {
        JsonNode payload = consultarTokenInfo(idToken);

        String aud = payload.path("aud").asText();
        String iss = payload.path("iss").asText();
        if (!googleClientId.equals(aud) || !GOOGLE_ISSUERS.contains(iss)) {
            // aud is a public client id; logging it helps spot a misconfigured GOOGLE_CLIENT_ID.
            log.warn("Google token rejected: audience or issuer mismatch (aud={}, iss={})", aud, iss);
            throw tokenInvalido();
        }

        String email = payload.path("email").asText();
        String sub = payload.path("sub").asText();
        boolean emailVerificado = "true".equalsIgnoreCase(payload.path("email_verified").asText());
        if (email.isBlank() || sub.isBlank() || !emailVerificado) {
            throw new UnauthorizedException("GOOGLE_EMAIL_NOT_VERIFIED",
                "A conta Google precisa ter o e-mail verificado.");
        }
        return new GoogleIdentity(email, sub, payload.path("name").asText());
    }

    private JsonNode consultarTokenInfo(String idToken) {
        try {
            // tokeninfo checks the signature and expiry on Google's side.
            String json = restTemplate.getForObject(GOOGLE_TOKEN_INFO + idToken, String.class);
            JsonNode payload = objectMapper.readTree(json);
            if (payload == null || payload.has("error_description")) {
                throw tokenInvalido();
            }
            return payload;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            // RestClientException, IOException, parsing errors — all funnel to the same response.
            log.warn("Google tokeninfo lookup failed: {}", e.getMessage());
            throw tokenInvalido();
        }
    }

    private void validarContaExistente(Usuario u, String sub) {
        if (u.getGoogleId() != null && !u.getGoogleId().equals(sub)) {
            throw new UnauthorizedException("GOOGLE_ACCOUNT_MISMATCH",
                "Este e-mail está vinculado a outra conta Google.");
        }
        if (!u.isEnabled()) {
            throw new UnauthorizedException("ACCOUNT_DISABLED", "Conta desativada.");
        }
        authService.assertNaoBloqueada(u);
    }

    private Usuario provisionar(GoogleIdentity identity) {
        Role role = roleRepo.findByNome(NEW_USER_ROLE)
            .orElseThrow(() -> new IllegalStateException("Required role missing: " + NEW_USER_ROLE));
        // A new Google sign-in bootstraps its own tenant so the user has an
        // isolated workspace from the first request.
        String nomeEmpresa = !identity.nome().isBlank() ? "Empresa de " + identity.nome() : "Minha Empresa";
        Empresa empresa = empresaRepo.save(new Empresa(nomeEmpresa));

        Usuario novo = new Usuario();
        novo.setEmail(identity.email());
        novo.setNome(identity.nome());
        novo.setGoogleId(identity.sub());
        // Random unguessable bcrypt hash — user never logs in with email/password here.
        novo.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
        novo.setRole(role);
        novo.setEmpresaId(empresa.getId());
        return usuarioRepo.save(novo);
    }

    private static UnauthorizedException tokenInvalido() {
        return new UnauthorizedException("GOOGLE_TOKEN_INVALID", "Token Google inválido.");
    }
}
