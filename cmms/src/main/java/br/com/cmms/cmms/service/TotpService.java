package br.com.cmms.cmms.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

@Service
public class TotpService {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TOTP_PERIOD = 30;
    private static final int TOTP_DIGITS = 6;
    private static final int WINDOW = 1; // allow ±1 window

    /** Generate a random 20-byte secret encoded as Base32 */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    /** Build the otpauth:// URI used for QR code generation */
    public String buildOtpAuthUri(String issuer, String accountName, String secret) {
        String encoded = secret.replace("=", "");
        return "otpauth://totp/" +
               uriEncode(issuer + ":" + accountName) +
               "?secret=" + encoded +
               "&issuer=" + uriEncode(issuer) +
               "&algorithm=SHA1&digits=" + TOTP_DIGITS + "&period=" + TOTP_PERIOD;
    }

    /** Verify a 6-digit TOTP code against the secret, allowing ±WINDOW periods */
    public boolean verifyCode(String secret, String code) {
        if (code == null || code.length() != TOTP_DIGITS) return false;
        long currentStep = System.currentTimeMillis() / 1000 / TOTP_PERIOD;
        for (long step = currentStep - WINDOW; step <= currentStep + WINDOW; step++) {
            if (generateCode(secret, step).equals(code)) return true;
        }
        return false;
    }

    private String generateCode(String secret, long timeStep) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] msg = ByteBuffer.allocate(8).putLong(timeStep).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(msg);
            int offset = hash[hash.length - 1] & 0x0F;
            int code = ((hash[offset]     & 0x7F) << 24)
                     | ((hash[offset + 1] & 0xFF) << 16)
                     | ((hash[offset + 2] & 0xFF) << 8)
                     |  (hash[offset + 3] & 0xFF);
            return String.format("%0" + TOTP_DIGITS + "d", code % (int) Math.pow(10, TOTP_DIGITS));
        } catch (Exception e) {
            throw new RuntimeException("TOTP generation failed", e);
        }
    }

    // ── Base32 codec ──────────────────────────────────────────────────────

    public static String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_CHARS.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }

    public static byte[] decodeBase32(String s) {
        s = s.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] result = new byte[s.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : s.toCharArray()) {
            int value = (c >= 'A') ? (c - 'A') : (c - '2' + 26);
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8 && idx < result.length) {
                result[idx++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return result;
    }

    private static String uriEncode(String s) {
        return s.replace("@", "%40").replace(":", "%3A").replace(" ", "%20").replace("+", "%2B");
    }
}
