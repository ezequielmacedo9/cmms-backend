package br.com.cmms.cmms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link RateLimitFilter} (per-IP, per-endpoint buckets). */
class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(new ObjectMapper());
    }

    @Test
    @DisplayName("endpoint sem politica passa direto (sem consumir token)")
    void semPolitica_passa() throws Exception {
        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(request("GET", "/api/maquinas", "10.0.0.1"), res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("login: 11a tentativa do mesmo IP leva 429 com envelope RATE_LIMITED")
    void login_estoura_no_11() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(request("POST", "/api/auth/login", "10.0.0.2"), res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/auth/login", "10.0.0.2"), bloqueada, new MockFilterChain());

        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getHeader("Retry-After")).isEqualTo("60");
        assertThat(bloqueada.getContentAsString()).contains("RATE_LIMITED");
    }

    @Test
    @DisplayName("buckets por IP: outro IP nao e penalizado; OPTIONS nunca consome")
    void ipsIndependentes_eOptionsBypass() throws Exception {
        for (int i = 0; i < 11; i++) {
            filter.doFilter(request("POST", "/api/auth/login", "10.0.0.3"),
                new MockHttpServletResponse(), new MockFilterChain());
        }
        // IP diferente continua com o proprio orcamento.
        MockHttpServletResponse outroIp = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/auth/login", "10.0.0.4"), outroIp, new MockFilterChain());
        assertThat(outroIp.getStatus()).isEqualTo(200);

        // Preflight CORS nao conta mesmo no IP estourado.
        MockHttpServletResponse preflight = new MockHttpServletResponse();
        filter.doFilter(request("OPTIONS", "/api/auth/login", "10.0.0.3"), preflight, new MockFilterChain());
        assertThat(preflight.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("X-Forwarded-For define o bucket (primeiro IP da cadeia)")
    void xForwardedFor_defineBucket() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = request("POST", "/api/auth/forgot-password", "127.0.0.1");
            req.addHeader("X-Forwarded-For", "200.1.2.3, 10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletRequest req = request("POST", "/api/auth/forgot-password", "127.0.0.1");
        req.addHeader("X-Forwarded-For", "200.1.2.3, 10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertThat(res.getStatus()).isEqualTo(429);

        // Mesmo remoteAddr mas outro cliente atras do proxy: bucket proprio.
        MockHttpServletRequest outro = request("POST", "/api/auth/forgot-password", "127.0.0.1");
        outro.addHeader("X-Forwarded-For", "200.9.9.9");
        MockHttpServletResponse resOutro = new MockHttpServletResponse();
        filter.doFilter(outro, resOutro, new MockFilterChain());
        assertThat(resOutro.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletRequest request(String method, String path, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setServletPath(path);
        req.setRemoteAddr(ip);
        return req;
    }
}
