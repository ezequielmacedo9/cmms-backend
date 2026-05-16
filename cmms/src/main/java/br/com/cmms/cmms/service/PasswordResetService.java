package br.com.cmms.cmms.service;

import br.com.cmms.cmms.exception.UnauthorizedException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.PasswordResetToken;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.PasswordResetTokenRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Password reset flow:
 * <ol>
 *   <li>{@link #requestReset(String, HttpServletRequest)} — generates a
 *       short-lived token and emails it. Always returns silently to prevent
 *       e-mail enumeration.</li>
 *   <li>{@link #isTokenValid(String)} — checks whether a token is still
 *       usable (not expired, not consumed).</li>
 *   <li>{@link #resetPassword(String, String, HttpServletRequest)} — consumes
 *       the token and replaces the user's password.</li>
 * </ol>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final int TOKEN_EXPIRY_HOURS = 1;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int TOKEN_BYTES = 32;

    private final UsuarioRepository usuarioRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UsuarioRepository usuarioRepo,
                                PasswordResetTokenRepository tokenRepo,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder,
                                AuditService audit) {
        this.usuarioRepo = usuarioRepo;
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    /** Generates a reset token and sends it by e-mail. Silent on unknown emails. */
    @Transactional
    public void requestReset(String email, HttpServletRequest request) {
        Optional<Usuario> opt = usuarioRepo.findByEmail(email);
        if (opt.isEmpty()) {
            log.debug("Password reset requested for unknown email; ignoring silently.");
            return;
        }
        Usuario u = opt.get();
        tokenRepo.deleteExpiredAndUsed(LocalDateTime.now());

        String rawToken = generateToken();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(rawToken);
        prt.setUsuario(u);
        prt.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        tokenRepo.save(prt);

        try {
            emailService.sendPasswordReset(u.getEmail(), rawToken, u.getNome());
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", u.getEmail(), e.getMessage());
        }
        audit.log(u.getEmail(), u.getId(), "PASSWORD_RESET_REQUEST", "USUARIO", null,
            "Solicitação de redefinição", AuditService.getClientIp(request));
    }

    public boolean isTokenValid(String token) {
        return tokenRepo.findByToken(token)
            .map(t -> !t.isUsado() && !t.isExpired())
            .orElse(false);
    }

    /**
     * Consumes the token and persists the new password.
     *
     * @throws UnauthorizedException if the token is missing, expired or already used
     * @throws ValidationException   if the new password fails the policy
     */
    @Transactional
    public void resetPassword(String token, String novaSenha, HttpServletRequest request) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("RESET_TOKEN_MISSING", "Token ausente.");
        }
        if (novaSenha == null || novaSenha.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("PASSWORD_TOO_SHORT",
                "Senha deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        PasswordResetToken prt = tokenRepo.findByToken(token).orElse(null);
        if (prt == null || prt.isUsado() || prt.isExpired()) {
            throw new UnauthorizedException("RESET_TOKEN_INVALID", "Token inválido ou expirado.");
        }

        Usuario u = prt.getUsuario();
        u.setSenha(passwordEncoder.encode(novaSenha));
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        usuarioRepo.save(u);

        prt.setUsado(true);
        tokenRepo.save(prt);

        audit.log(u.getEmail(), u.getId(), "PASSWORD_RESET", "USUARIO", u.getId(),
            "Senha redefinida via token", AuditService.getClientIp(request));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
