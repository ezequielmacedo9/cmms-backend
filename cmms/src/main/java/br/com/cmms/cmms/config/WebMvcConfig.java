package br.com.cmms.cmms.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

/**
 * Adds the canonical {@code /api/v1/*} prefix on top of every legacy
 * {@code /api/*} mapping, without breaking any existing client.
 *
 * <p>Implementation: a tiny servlet filter that rewrites incoming URIs
 * matching {@code /api/v1/...} to {@code /api/...} before Spring dispatches.
 * Controllers keep declaring {@code @RequestMapping("/api/...")}; both
 * paths work simultaneously. When clients have fully migrated, this filter
 * is the single thing to remove.
 *
 * <p>This is preferred over duplicating controllers or using
 * {@code addPathPrefix} because it leaves the Swagger paths, security
 * configuration and integration tests untouched.
 */
@Configuration
public class WebMvcConfig {

    private static final String V1_PREFIX = "/api/v1/";
    private static final String LEGACY_PREFIX = "/api/";

    /**
     * Filter that rewrites {@code /api/v1/foo} -> {@code /api/foo}. Runs
     * before security so authn / rate-limit policies see the canonical
     * path used in {@code SecurityConfig} and {@code RateLimitFilter}.
     */
    @Bean
    public FilterRegistrationBean<Filter> apiVersionRewriteFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(new V1RewriteFilter());
        bean.addUrlPatterns("/api/v1/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        bean.setName("apiV1RewriteFilter");
        return bean;
    }

    /** Strips the {@code /api/v1} prefix so legacy mappings match transparently. */
    private static final class V1RewriteFilter implements Filter {
        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
            if (req instanceof HttpServletRequest http) {
                String uri = http.getRequestURI();
                if (uri.startsWith(V1_PREFIX)) {
                    String rewritten = LEGACY_PREFIX + uri.substring(V1_PREFIX.length());
                    chain.doFilter(new RewrittenRequest(http, rewritten), res);
                    return;
                }
            }
            chain.doFilter(req, res);
        }
    }

    /**
     * Lightweight wrapper that overrides {@code requestURI}, {@code servletPath}
     * and {@code pathInfo} so downstream filters (security, MVC dispatcher)
     * see the rewritten path. Query string and headers are untouched.
     */
    private static final class RewrittenRequest extends HttpServletRequestWrapper {
        private final String rewritten;
        RewrittenRequest(HttpServletRequest delegate, String rewritten) {
            super(delegate);
            this.rewritten = rewritten;
        }
        @Override public String getRequestURI()  { return rewritten; }
        @Override public String getServletPath() { return rewritten; }
        @Override public String getPathInfo()    { return null; }
    }
}
