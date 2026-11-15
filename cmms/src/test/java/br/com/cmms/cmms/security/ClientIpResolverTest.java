package br.com.cmms.cmms.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    @DisplayName("sem cabecalhos do proxy usa o endereco do socket")
    void resolve_noProxyHeaders_usesRemoteAddr() {
        assertThat(ClientIpResolver.resolve(request("10.0.0.7"))).isEqualTo("10.0.0.7");
    }

    @Test
    @DisplayName("True-Client-IP (Cloudflare) tem prioridade sobre CF-Connecting-IP")
    void resolve_trueClientIp_takesPriority() {
        MockHttpServletRequest req = request("10.0.0.7");
        req.addHeader("True-Client-IP", "200.1.2.3");
        req.addHeader("CF-Connecting-IP", "200.4.5.6");

        assertThat(ClientIpResolver.resolve(req)).isEqualTo("200.1.2.3");
    }

    @Test
    @DisplayName("CF-Connecting-IP e usado quando nao ha True-Client-IP (inclui IPv6)")
    void resolve_cfConnectingIp_usedAsFallback() {
        MockHttpServletRequest req = request("10.0.0.7");
        req.addHeader("CF-Connecting-IP", "2804:14c:5b:8000::1");

        assertThat(ClientIpResolver.resolve(req)).isEqualTo("2804:14c:5b:8000::1");
    }

    @Test
    @DisplayName("X-Forwarded-For e ignorado: o primeiro valor e escolhido pelo cliente")
    void resolve_xForwardedFor_isIgnored() {
        MockHttpServletRequest req = request("10.0.0.7");
        req.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.1");

        assertThat(ClientIpResolver.resolve(req)).isEqualTo("10.0.0.7");
    }

    @Test
    @DisplayName("valor que nao parece IP e descartado")
    void resolve_malformedHeader_fallsBackToRemoteAddr() {
        MockHttpServletRequest req = request("10.0.0.7");
        req.addHeader("True-Client-IP", "<script>alert(1)</script>");

        assertThat(ClientIpResolver.resolve(req)).isEqualTo("10.0.0.7");
    }

    private static MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr(remoteAddr);
        return req;
    }
}
