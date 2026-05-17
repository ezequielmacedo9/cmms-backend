package br.com.cmms.cmms.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Propagates a request-level trace identifier so the same id appears in
 * application logs (via MDC), error responses (via {@code ApiError.traceId})
 * and the {@code X-Trace-Id} response header — making cross-tier debugging
 * trivial.
 *
 * <p>Order: must run before {@code JwtAuthFilter}, so anything logged or
 * thrown during authentication already has a traceId.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** Inbound header — if the client provides one, we adopt it. */
    public static final String INBOUND_HEADER = "X-Trace-Id";
    /** Outbound header — always set on the response. */
    public static final String RESPONSE_HEADER = "X-Trace-Id";
    /** MDC key consumed by logback-spring.xml. */
    public static final String MDC_KEY = "traceId";

    private static final int MAX_INBOUND_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = sanitize(request.getHeader(INBOUND_HEADER));
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, traceId);
        response.setHeader(RESPONSE_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Defensive: reject anything that smells like log injection or an
     * unreasonable identifier length. {@code null} signals "generate a new one".
     */
    private static String sanitize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_INBOUND_LENGTH) return null;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean ok =
                (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') ||
                c == '-' || c == '_' || c == '.';
            if (!ok) return null;
        }
        return trimmed;
    }
}
