package br.com.cmms.cmms.Security;

import br.com.cmms.cmms.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Minimum key length in bytes for HS256 (RFC 7518 §3.2). */
    private static final int MIN_KEY_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey signingKey;

    @PostConstruct
    void initSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable is required and must not be blank. " +
                "Generate one with: openssl rand -base64 48"
            );
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least " + MIN_KEY_BYTES +
                " bytes (256 bits) for HS256. Current length: " + keyBytes.length + " bytes."
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT signing key initialized ({} bytes).", keyBytes.length);
    }

    private SecretKey getSignKey() {
        return signingKey;
    }

    public String gerarToken(Usuario usuario) {
        return Jwts.builder()
                .setSubject(usuario.getEmail())
                .claim("role", usuario.getRole().getNome())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public String extrairRole(String token) {
        return extrairClaims(token).get("role", String.class);
    }

    private Claims extrairClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            throw e;
        }
    }
}
