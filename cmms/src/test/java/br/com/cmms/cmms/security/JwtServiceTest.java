package br.com.cmms.cmms.security;

import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService} — focused on token shape, claims and
 * negative paths (tampered signature, expired token, weak/blank secret).
 *
 * <p>The service reads its secret from {@code @Value}, so we wire it via
 * {@link ReflectionTestUtils} and call {@code initSigningKey()} ourselves
 * rather than spinning up the Spring context.
 */
class JwtServiceTest {

    private static final String VALID_SECRET = "test-secret-32bytes-or-longer-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = newJwtService(VALID_SECRET, ONE_HOUR_MS);
    }

    @Test
    @DisplayName("init: rejeita secret nulo")
    void init_rejectsNullSecret() {
        JwtService blank = new JwtService();
        ReflectionTestUtils.setField(blank, "secret", null);
        ReflectionTestUtils.setField(blank, "expiration", ONE_HOUR_MS);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(blank, "initSigningKey"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("init: rejeita secret em branco")
    void init_rejectsBlankSecret() {
        JwtService blank = new JwtService();
        ReflectionTestUtils.setField(blank, "secret", "   ");
        ReflectionTestUtils.setField(blank, "expiration", ONE_HOUR_MS);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(blank, "initSigningKey"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("init: rejeita secret abaixo de 32 bytes (HS256)")
    void init_rejectsShortSecret() {
        JwtService weak = new JwtService();
        ReflectionTestUtils.setField(weak, "secret", "short");
        ReflectionTestUtils.setField(weak, "expiration", ONE_HOUR_MS);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(weak, "initSigningKey"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("256 bits");
    }

    @Test
    @DisplayName("gerarToken: emite JWT com sub=email e claim role")
    void gerarToken_setsSubjectAndRoleClaim() {
        Usuario u = sampleUser("ana@cmms.app", "ROLE_GESTOR");

        String token = jwtService.gerarToken(u);

        assertThat(token).isNotBlank().contains(".");
        assertThat(jwtService.extrairEmail(token)).isEqualTo("ana@cmms.app");
        assertThat(jwtService.extrairRole(token)).isEqualTo("ROLE_GESTOR");
    }

    @Test
    @DisplayName("extrair: token assinado com outra chave é rejeitado")
    void extrair_rejectsTamperedSignature() {
        String token = jwtService.gerarToken(sampleUser("bob@cmms.app", "ROLE_TECNICO"));

        // Service with a *different* signing key — should reject the token.
        JwtService other = newJwtService(
            "another-secret-32bytes-long-zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz",
            ONE_HOUR_MS);

        assertThatThrownBy(() -> other.extrairEmail(token))
            .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("extrair: token expirado dispara ExpiredJwtException")
    void extrair_rejectsExpiredToken() {
        // Token life = 1ms so the assertion runs well after expiration.
        JwtService shortLived = newJwtService(VALID_SECRET, 1L);
        String token = shortLived.gerarToken(sampleUser("carol@cmms.app", "ROLE_VISUALIZADOR"));

        // Give the JVM time to walk past the exp claim.
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertThatThrownBy(() -> shortLived.extrairEmail(token))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("extrair: lixo claramente malformado é rejeitado")
    void extrair_rejectsGarbage() {
        assertThatThrownBy(() -> jwtService.extrairEmail("not-a-jwt"))
            .isInstanceOfAny(MalformedJwtException.class, IllegalArgumentException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static JwtService newJwtService(String secret, long expirationMs) {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "secret", secret);
        ReflectionTestUtils.setField(svc, "expiration", expirationMs);
        ReflectionTestUtils.invokeMethod(svc, "initSigningKey");
        return svc;
    }

    private static Usuario sampleUser(String email, String roleName) {
        Usuario u = new Usuario();
        u.setEmail(email);
        Role r = new Role(roleName);
        u.setRole(r);
        return u;
    }
}
