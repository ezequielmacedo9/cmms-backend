package br.com.cmms.cmms.service;

import br.com.cmms.cmms.exception.UnauthorizedException;
import br.com.cmms.cmms.model.RefreshToken;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Owns the lifecycle of {@link RefreshToken}s.
 *
 * <p>Refresh tokens are opaque UUIDs persisted in the database. They:
 * <ul>
 *   <li>Live for 7 days from issuance.</li>
 *   <li>Are <strong>rotated</strong> on every successful refresh — the old
 *       token is invalidated atomically and a new one is issued.
 *       This way a token captured in transit only works once.</li>
 *   <li>Can be globally revoked via {@link #revogarTodos(Usuario)}, which is
 *       what {@code POST /api/auth/logout} calls.</li>
 * </ul>
 */
@Service
@Transactional
public class RefreshTokenService {

    private static final int LIFETIME_DAYS = 7;

    private final RefreshTokenRepository repository;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Issues a fresh refresh token, replacing any previous one for the user.
     * Used at successful login.
     */
    public RefreshToken criarRefreshToken(Usuario usuario) {
        repository.deleteByUsuarioId(usuario.getId());
        repository.flush();
        return saveNew(usuario);
    }

    /**
     * Validates the supplied refresh token and, if good, replaces it with a
     * new one (rotation). The returned object always contains the
     * <em>new</em> token; the old one is gone.
     *
     * @throws UnauthorizedException when the token is missing, unknown or expired
     */
    public RefreshToken rotacionar(String oldToken) {
        RefreshToken current = validar(oldToken);
        Usuario owner = current.getUsuario();
        repository.delete(current);
        repository.flush();
        return saveNew(owner);
    }

    /**
     * Read-only validation. Use {@link #rotacionar(String)} when issuing a
     * new access token so the refresh is consumed atomically.
     */
    public RefreshToken validar(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("REFRESH_TOKEN_MISSING", "Refresh token ausente.");
        }
        RefreshToken refreshToken = repository.findByToken(token)
            .orElseThrow(() -> new UnauthorizedException("REFRESH_TOKEN_INVALID", "Refresh token inválido."));

        if (refreshToken.getExpiracao().isBefore(Instant.now())) {
            repository.delete(refreshToken);
            throw new UnauthorizedException("REFRESH_TOKEN_EXPIRED", "Refresh token expirado.");
        }
        return refreshToken;
    }

    /** Wipes every refresh token a user has. Called on logout / password reset. */
    public void revogarTodos(Usuario usuario) {
        repository.deleteByUsuarioId(usuario.getId());
    }

    private RefreshToken saveNew(Usuario owner) {
        RefreshToken rt = new RefreshToken();
        rt.setUsuario(owner);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiracao(Instant.now().plus(LIFETIME_DAYS, ChronoUnit.DAYS));
        return repository.save(rt);
    }
}
