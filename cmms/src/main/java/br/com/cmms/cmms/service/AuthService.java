package br.com.cmms.cmms.service;

import br.com.cmms.cmms.security.JwtService;
import br.com.cmms.cmms.security.UserDetailsImpl;
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
    private final TotpService totpService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UsuarioRepository usuarioRepo,
                       ConfiguracaoService config,
                       AuditService audit,
                       TotpService totpService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.usuarioRepo = usuarioRepo;
        this.config = config;
        this.audit = audit;
        this.totpService = totpService;
    }

    /**
     * Authenticates with email + password, applying lockout policy. Returns
     * a fresh {@link TokenResponseDTO} with access and refresh tokens.
     *
     * <p><strong>2FA:</strong> when the user has TOTP enabled, a valid
     * {@code totpCode} is required — a correct password alone returns
     * {@code TOTP_REQUIRED} so the client can prompt for the code. When the
     * empresa enforces 2FA ({@code seguranca.2fa.obrigatorio}) and the user
     * hasn't enrolled, login succeeds with {@code twoFactorSetupRequired=true}.
     *
     * @throws ForbiddenException    when the account is currently locked
     * @throws UnauthorizedException when the credentials don't match (or the user doesn't exist)
     */
    // noRollbackFor: a failed attempt must still commit the incremented counter
    // and the lockout timestamp. With the default rollback rules the exception
    // that reports the failure would also undo it, so the account never locked.
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponseDTO login(String email, String senha, String totpCode, HttpServletRequest request) {
        log.info("Login attempt: {}", email);
        Optional<Usuario> opt = usuarioRepo.findByEmail(email);

        if (opt.isEmpty()) {
            // Same response as bad password to prevent user enumeration.
            throw new UnauthorizedException("BAD_CREDENTIALS", "Email ou senha incorretos.");
        }

        Usuario u = opt.get();
        assertNaoBloqueada(u);

        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, senha)
            );
            u = ((UserDetailsImpl) auth.getPrincipal()).getUsuario();
        } catch (BadCredentialsException e) {
            handleFailedAttempt(u, request, "Senha incorreta");
            throw new UnauthorizedException("BAD_CREDENTIALS", "Email ou senha incorretos.");
        }

        verificarSegundoFator(u, totpCode, request);
        return concluirLogin(u, "LOGIN", "Login bem-sucedido", request);
    }

    /**
     * Refuses an account that is currently locked. Shared by every login flow.
     *
     * @throws ForbiddenException when the lockout window is still open
     */
    public void assertNaoBloqueada(Usuario u) {
        if (!u.isAccountNonLocked()) {
            throw new ForbiddenException("ACCOUNT_LOCKED",
                "Conta bloqueada. Tente novamente em alguns minutos.");
        }
    }

    /**
     * Second-factor gate shared by the password and Google flows. When the
     * user has TOTP enabled, a missing code returns {@code TOTP_REQUIRED} and a
     * wrong one counts as a failed attempt.
     *
     * <p>Not transactional on purpose: it runs inside the caller's transaction,
     * whose {@code noRollbackFor} keeps the failed-attempt counter written here.
     */
    public void verificarSegundoFator(Usuario u, String totpCode, HttpServletRequest request) {
        if (!u.isTotpEnabled()) {
            return;
        }
        if (totpCode == null || totpCode.isBlank()) {
            // First factor was fine — ask the client for the second one.
            throw new UnauthorizedException("TOTP_REQUIRED", "Informe o código do autenticador (2FA).");
        }
        if (!totpService.verifyCode(u.getTotpSecret(), totpCode)) {
            // A wrong code counts toward lockout, otherwise a leaked
            // password lets an attacker brute-force the 6 digits.
            handleFailedAttempt(u, request, "Código 2FA inválido");
            throw new UnauthorizedException("TOTP_INVALID", "Código 2FA inválido.");
        }
    }

    /**
     * Successful-login bookkeeping shared by every flow: resets the lockout
     * counters, stamps the last login, issues access + refresh tokens, audits
     * and flags {@code twoFactorSetupRequired} when the empresa enforces 2FA.
     */
    public TokenResponseDTO concluirLogin(Usuario u, String acao, String detalhe, HttpServletRequest request) {
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        u.setUltimoLogin(LocalDateTime.now());
        usuarioRepo.save(u);

        String accessToken  = jwtService.gerarToken(u);
        String refreshToken = refreshTokenService.criarRefreshToken(u).getToken();

        audit.log(u.getEmpresaId(), u.getEmail(), u.getId(), acao, "AUTH", null,
            detalhe, AuditService.getClientIp(request));

        TokenResponseDTO response = buildResponse(u, accessToken, refreshToken);
        if (!u.isTotpEnabled() && config.getBool(u.getEmpresaId(), "seguranca.2fa.obrigatorio")) {
            response.setTwoFactorSetupRequired(true);
        }
        return response;
    }

    /**
     * Exchanges a valid refresh token for a brand-new access + refresh pair.
     *
     * <p><strong>Rotation:</strong> the old refresh token is invalidated
     * atomically inside {@link RefreshTokenService#rotacionar(String)}. A
     * leaked refresh token only works once — the next refresh attempt with
     * the same value returns {@code REFRESH_TOKEN_INVALID}.
     */
    @Transactional
    public TokenResponseDTO refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("REFRESH_TOKEN_MISSING", "Refresh token ausente.");
        }
        RefreshToken rotated = refreshTokenService.rotacionar(refreshToken);
        Usuario u = rotated.getUsuario();
        String novoAccessToken = jwtService.gerarToken(u);
        return buildResponse(u, novoAccessToken, rotated.getToken());
    }

    /**
     * Logs the user out by revoking all of their refresh tokens. The
     * already-issued access token continues to work until it expires
     * naturally (≤15min by default) — that's the trade-off of stateless
     * JWTs; we mitigate it by keeping access TTL short.
     *
     * <p>Safe to call multiple times.
     */
    @Transactional
    public void logout(Usuario u, HttpServletRequest request) {
        if (u == null) return;
        refreshTokenService.revogarTodos(u);
        audit.log(u.getEmpresaId(), u.getEmail(), u.getId(), "LOGOUT", "AUTH", null,
            "Logout / refresh tokens revogados", AuditService.getClientIp(request));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void handleFailedAttempt(Usuario u, HttpServletRequest request, String motivo) {
        int attempts = u.getFailedLoginAttempts() + 1;
        int maxAttempts = config.getInt(u.getEmpresaId(), "seguranca.lockout.tentativas", DEFAULT_LOCKOUT_ATTEMPTS);
        u.setFailedLoginAttempts(attempts);
        boolean lockedNow = false;
        if (attempts >= maxAttempts) {
            int lockMinutes = config.getInt(u.getEmpresaId(), "seguranca.lockout.minutos", DEFAULT_LOCKOUT_MINUTES);
            u.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
            lockedNow = true;
            log.warn("Account locked: {} after {} attempts", u.getEmail(), attempts);
        }
        usuarioRepo.save(u);
        String ip = AuditService.getClientIp(request);
        audit.log(u.getEmpresaId(), u.getEmail(), u.getId(), "LOGIN_FAILED", "AUTH", null,
            motivo + " (tentativa " + attempts + ")", ip);
        if (lockedNow) {
            // Separate event so security dashboards can alert on lockouts
            // without trawling failed-login noise.
            audit.log(u.getEmpresaId(), u.getEmail(), u.getId(), "ACCOUNT_LOCKED", "AUTH", null,
                "Conta bloqueada após " + attempts + " tentativas", ip);
        }
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
