package br.com.cmms.cmms.Security;

import br.com.cmms.cmms.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "Chave JWT inválida! Deve ter pelo menos 32 bytes (256 bits). Atual: " + keyBytes.length + " bytes. " +
                            "Atualize jwt.secret no application.properties para uma string mais longa."
            );
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String gerarToken(Usuario usuario) {
        return Jwts.builder()
                .setSubject(usuario.getEmail()) // veja ajuste abaixo
                .claim("role", usuario.getRole().getNome())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
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
        System.out.println("JwtService: Tentando parsear token...");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            System.out.println("JwtService: Token parseado com sucesso. Subject: " + claims.getSubject());
            return claims;
        } catch (Exception e) {
            System.out.println("JwtService: Erro ao parsear token: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw e;
        }
    }
}
