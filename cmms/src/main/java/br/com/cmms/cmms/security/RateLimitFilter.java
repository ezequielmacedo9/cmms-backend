package br.com.cmms.cmms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Per-IP rate limiting for security-sensitive endpoints. Each endpoint has
 * its own policy (capacity + refill window) so we throttle realistically:
 *
 * <table>
 *   <tr><th>Endpoint</th>            <th>Capacity</th> <th>Refill</th>     <th>Why</th></tr>
 *   <tr><td>POST /api/auth/login</td>           <td>10</td>  <td>1 min</td>  <td>credential stuffing</td></tr>
 *   <tr><td>POST /api/auth/google</td>          <td>20</td>  <td>1 min</td>  <td>token stuffing</td></tr>
 *   <tr><td>POST /api/auth/refresh</td>         <td>30</td>  <td>1 min</td>  <td>refresh thrash</td></tr>
 *   <tr><td>POST /api/auth/forgot-password</td> <td>3</td>   <td>5 min</td>  <td>email spam / SMTP cost</td></tr>
 *   <tr><td>POST /api/auth/reset-password</td>  <td>10</td>  <td>5 min</td>  <td>brute force on token</td></tr>
 *   <tr><td>GET  /api/auth/validate-reset-token</td><td>30</td><td>1 min</td><td>token enumeration</td></tr>
 * </table>
 *
 * <p>Buckets are scoped by (IP, endpoint) — abusive traffic on one path
 * doesn't poison the budget of another. Storage is a Caffeine cache with
 * an idle-expiry policy so the map can't grow without bound (memory leak
 * of the previous implementation).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    /** Identity for buckets inside the Caffeine cache. */
    private record BucketKey(String policy, String ip) {}

    /** Endpoint policy: which requests match it and what bucket params to use. */
    private record Policy(String name,
                          Predicate<HttpServletRequest> matcher,
                          int capacity,
                          Duration refillPeriod) {}

    private static final List<Policy> POLICIES = List.of(
        new Policy("login",          matches("POST", "/api/auth/login"),                10, Duration.ofMinutes(1)),
        new Policy("register",       matches("POST", "/api/auth/register"),              5, Duration.ofMinutes(10)),
        new Policy("google",         matches("POST", "/api/auth/google"),               20, Duration.ofMinutes(1)),
        new Policy("refresh",        matches("POST", "/api/auth/refresh"),              30, Duration.ofMinutes(1)),
        new Policy("forgot",         matches("POST", "/api/auth/forgot-password"),       3, Duration.ofMinutes(5)),
        new Policy("reset",          matches("POST", "/api/auth/reset-password"),       10, Duration.ofMinutes(5)),
        new Policy("validate-reset", matches("GET",  "/api/auth/validate-reset-token"), 30, Duration.ofMinutes(1))
    );

    private final Cache<BucketKey, Bucket> buckets = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(15))
        .maximumSize(50_000)
        .build();

    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // CORS preflights must not consume tokens.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Policy policy = matchPolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        Bucket bucket = buckets.get(new BucketKey(policy.name(), ip), k -> newBucket(policy));
        Objects.requireNonNull(bucket, "bucket");

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }
        writeTooMany(request, response, policy);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static Bucket newBucket(Policy p) {
        Bandwidth limit = Bandwidth.classic(p.capacity(), Refill.intervally(p.capacity(), p.refillPeriod()));
        return Bucket.builder().addLimit(limit).build();
    }

    private Policy matchPolicy(HttpServletRequest request) {
        for (Policy p : POLICIES) if (p.matcher().test(request)) return p;
        return null;
    }

    private static Predicate<HttpServletRequest> matches(String method, String path) {
        return req -> method.equalsIgnoreCase(req.getMethod()) && path.equals(req.getServletPath());
    }

    /**
     * Extracts the originating client IP. We only trust {@code X-Forwarded-For}
     * when the {@code app.proxy.trusted} flag is on (set in prod where the
     * platform — Render, Vercel, Cloudflare — sits in front and always sets
     * the header). In other environments fall back to the socket address.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooMany(HttpServletRequest request, HttpServletResponse response, Policy policy)
        throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // RFC 6585 / RFC 7231 — hint clients to back off; conservative single value.
        response.setHeader("Retry-After", String.valueOf(policy.refillPeriod().toSeconds()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status",    HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error",     "Too Many Requests");
        body.put("code",      "RATE_LIMITED");
        body.put("message",   "Muitas tentativas. Tente novamente em alguns instantes.");
        body.put("path",      request.getRequestURI());
        String traceId = MDC.get("traceId");
        if (traceId != null) body.put("traceId", traceId);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
