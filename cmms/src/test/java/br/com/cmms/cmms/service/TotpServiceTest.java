package br.com.cmms.cmms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link TotpService} (RFC 6238 helper). */
class TotpServiceTest {

    private final TotpService service = new TotpService();

    @Test
    @DisplayName("generateSecret: 20 bytes -> 32 chars Base32, roundtrip decode")
    void generateSecret_roundtrip() {
        String secret = service.generateSecret();
        assertThat(secret).hasSize(32).matches("[A-Z2-7]+");
        assertThat(TotpService.decodeBase32(secret)).hasSize(20);
    }

    @Test
    @DisplayName("encode/decodeBase32: roundtrip preserva os bytes")
    void base32_roundtrip() {
        byte[] data = "hello-totp-secret!!!".getBytes(StandardCharsets.UTF_8);
        String encoded = TotpService.encodeBase32(data);
        assertThat(TotpService.decodeBase32(encoded)).isEqualTo(data);
    }

    @Test
    @DisplayName("verifyCode: rejeita nulo, tamanho errado e codigo aleatorio")
    void verifyCode_rejeitaMalformados() {
        String secret = service.generateSecret();
        assertThat(service.verifyCode(secret, null)).isFalse();
        assertThat(service.verifyCode(secret, "12345")).isFalse();
        assertThat(service.verifyCode(secret, "1234567")).isFalse();
    }

    @Test
    @DisplayName("verifyCode: aceita o codigo do periodo atual (RFC 6238)")
    void verifyCode_aceitaCodigoAtual() throws Exception {
        String secret = service.generateSecret();
        String code = rfc6238Code(secret, System.currentTimeMillis() / 1000 / 30);
        assertThat(service.verifyCode(secret, code)).isTrue();
    }

    @Test
    @DisplayName("buildOtpAuthUri: monta URI com issuer, conta e secret")
    void buildOtpAuthUri_formato() {
        String uri = service.buildOtpAuthUri("CMMS", "ana@empresa.com", "ABC234==");
        assertThat(uri)
            .startsWith("otpauth://totp/CMMS%3Aana%40empresa.com")
            .contains("secret=ABC234")
            .contains("issuer=CMMS")
            .contains("digits=6")
            .contains("period=30");
    }

    /** Independent RFC 6238 implementation to cross-check the service. */
    private static String rfc6238Code(String base32Secret, long timeStep) throws Exception {
        byte[] key = TotpService.decodeBase32(base32Secret);
        byte[] msg = ByteBuffer.allocate(8).putLong(timeStep).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(msg);
        int offset = hash[hash.length - 1] & 0x0F;
        int bin = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
        return String.format("%06d", bin % 1_000_000);
    }
}
