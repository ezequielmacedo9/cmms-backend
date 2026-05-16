package br.com.cmms.cmms.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates the Bearer JWT, loads the matching {@link UserDetails} from the
 * database (with a short Caffeine cache to avoid hitting the DB on every
 * request) and honours the account status flags ({@code enabled},
 * {@code accountNonLocked}). A disabled or locked user is rejected even if
 * the token is still cryptographically valid.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    /** Short TTL keeps the request path fast while still propagating account-status changes quickly. */
    private static final Duration USER_CACHE_TTL = Duration.ofSeconds(30);
    private static final int USER_CACHE_MAX = 5_000;

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    private final Cache<String, UserDetails> userCache = Caffeine.newBuilder()
        .expireAfterWrite(USER_CACHE_TTL)
        .maximumSize(USER_CACHE_MAX)
        .build();

    public JwtAuthFilter(JwtService jwtService,
                         UserDetailsService userDetailsService,
                         ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/ping")
            || path.equals("/error")
            || path.startsWith("/api/auth")
            || path.startsWith("/h2-console")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/actuator/health")
            || path.startsWith("/actuator/info");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        final String email;
        try {
            email = jwtService.extrairEmail(token);
        } catch (Exception e) {
            // Signature / expiration / format failure — log at debug to avoid log spam.
            log.debug("JWT rejected: {}", e.getMessage());
            writeUnauthorized(request, response, "INVALID_TOKEN", "Token inválido ou expirado.");
            return;
        }

        if (email == null || email.isBlank()) {
            writeUnauthorized(request, response, "INVALID_TOKEN", "Token sem subject.");
            return;
        }

        UserDetails user;
        try {
            user = userCache.get(email, this::loadUserOrNull);
        } catch (Exception e) {
            log.warn("Failed to load user details for {}: {}", email, e.getMessage());
            writeUnauthorized(request, response, "AUTH_LOOKUP_FAILED", "Falha ao validar credenciais.");
            return;
        }

        if (user == null) {
            writeUnauthorized(request, response, "USER_NOT_FOUND", "Usuário não encontrado.");
            return;
        }
        if (!user.isEnabled()) {
            writeUnauthorized(request, response, "ACCOUNT_DISABLED", "Conta desativada.");
            return;
        }
        if (!user.isAccountNonLocked()) {
            writeUnauthorized(request, response, "ACCOUNT_LOCKED", "Conta temporariamente bloqueada.");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /** Returns {@code null} on lookup failure so the Caffeine miss is not cached as an exception. */
    private UserDetails loadUserOrNull(String email) {
        try {
            return userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String code,
                                   String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("code", code);
        body.put("message", message);
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }
}
