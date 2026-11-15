package br.com.cmms.cmms.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolves the real client IP used by rate limiting and audit logs.
 *
 * <p>Render serves traffic through its Cloudflare edge, which overwrites
 * {@code True-Client-IP} / {@code CF-Connecting-IP} with the address it
 * actually saw, so the caller cannot forge them. {@code X-Forwarded-For} is
 * deliberately ignored: the proxy only appends to whatever the client sent,
 * so its first entry is attacker-controlled and would let a single client
 * get a fresh rate-limit bucket on every request.
 *
 * <p>If the API is ever deployed behind a different proxy, revisit
 * {@link #TRUSTED_HEADERS}.
 */
public final class ClientIpResolver {

    /** Headers written by the edge proxy (Cloudflare in front of Render), in priority order. */
    static final List<String> TRUSTED_HEADERS = List.of("True-Client-IP", "CF-Connecting-IP");

    /** IPv4/IPv6 literal: hex digits, dots and colons only. Rejects garbage keys. */
    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        for (String header : TRUSTED_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && IP_LITERAL.matcher(value.trim()).matches()) {
                return value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
