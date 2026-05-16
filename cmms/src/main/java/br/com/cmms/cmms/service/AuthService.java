package br.com.cmms.cmms.service;

import br.com.cmms.cmms.Security.JwtService;
import br.com.cmms.cmms.Security.UserDetailsImpl;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.exception.ForbiddenException;
import br.com.cmms.cmms.exception.UnauthorizedException;
import br.com.cmms.cmms.model.RefreshToken;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Authentication use cases for the e-mail/password flow.
 *
 * <p>Encapsulates everything that used to live in {@code AuthController}:
 * <ul>
 *   <li>Lockout enforcement and failed-attempts counter.</li>
 *   <li>Successful login bookkeeping (reset counters, stamp lastLogin).</li>
 *   <li>Access + refresh token issuance.</li>
 *   <li>Refresh token rotation.</li>
 * </ul>
 * Throws domain exceptions; HTTP status is decided by {@code GlobalExceptionHandler}.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int DEFAULT_LOCKOUT_ATTEMPTS = 5;
    private static final int DEFAULT_LOCKOUT_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioRepository usuarioRepo;
    private final ConfiguracaoService config;
    private final AuditService audit;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UsuarioRepository usuarioRepo,
                       ConfiguracaoService config,
                       AuditService audit) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.usuarioRepo = usuarioRepo;
        this.config = config;
        this.audit = audit;
    }

    /**
     * Authenticates with email + password, applying lockout policy. Returns
     * a fresh {@link TokenResponseDTO} with access and refresh tokens.
     *
     * @throws ForbiddenException    when the account is currently locked
     * @throws UnauthorizedException when the credentials don't match (or the user doesn't exist)
     */
    @Transactional
    public TokenResponseDTO login(String email, String senha, HttpServletRequest request) {
        log.info("Login attempt: {}", email);
        Optional<Usuario> opt = usuarioRepo.findByEmail(email);

        if (opt.isEmpty()) {
            // Same response as bad password to prevent user enumeration.
            throw new UnauthorizedException("BAD_CREDENTIALS", "Email ou senha incorretos.");
        }

        Usuario u = opt.get();

        if (!u.isAccountNonLocked()) {
            throw new ForbiddenException("ACCOUNT_LOCKED",
                "Conta bloqueada. Tente novamente em alguns minutos.");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, senha)
            );
            UserDetailsImpl principal = (UserDetailsImpl) auth.getPrincipal();
            u = principal.getUsuario();

            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            u.setUltimoLogin(LocalDateTime.now());
            usuarioRepo.save(u);

            String accessToken  = jwtService.gerarToken(u);
            String refreshToken = refreshTokenService.criarRefreshToken(u).getToken();

            audit.log(u.getEmail(), u.getId(), "LOGIN", "AUTH", null,
                "Login bem-sucedido", AuditService.getClientIp(request));

            return buildResponse(u, accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            handleFailedAttempt(u, request);
            throw new UnauthorizedException("BAD_CREDENTIALS", "Email ou senha incorretos.");
        }
    }

    /**
     * Exchanges a valid refresh token for a new access token. Refresh token
     * itself is preserved; rotation can be added later if needed.
     */
    @Transactional
    public TokenResponseDTO refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("REFRESH_TOKEN_MISSING", "Refresh token ausente.");
        }
        RefreshToken token = refreshTokenService.validar(refreshToken);
        Usuario u = token.getUsuario();
        String novoAccessToken = jwtService.gerarToken(u);
        return buildResponse(u, novoAccessToken, token.getToken());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void handleFailedAttempt(Usuario u, HttpServletRequest request) {
        int attempts = u.getFailedLoginAttempts() + 1;
        int maxAttempts = config.getInt("seguranca.lockout.tentativas", DEFAULT_LOCKOUT_ATTEMPTS);
        u.setFailedLoginAttempts(attempts);
        if (attempts >= maxAttempts) {
            int lockMinutes = config.getInt("seguranca.lockout.minutos", DEFAULT_LOCKOUT_MINUTES);
            u.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
            log.warn("Account locked: {} after {} attempts", u.getEmail(), attempts);
        }
        usuarioRepo.save(u);
        audit.log(u.getEmail(), u.getId(), "LOGIN_FAILED", "AUTH", null,
            "Tentativa " + attempts, AuditService.getClientIp(request));
    }

    private TokenResponseDTO buildResponse(Usuario u, String accessToken, String refreshToken) {
        return new TokenResponseDTO(
            accessToken, refreshToken,
            u.getRole().getNome(),
            u.getNome() != null ? u.getNome() : u.getEmail(),
            u.getId()
        );
    }
}
