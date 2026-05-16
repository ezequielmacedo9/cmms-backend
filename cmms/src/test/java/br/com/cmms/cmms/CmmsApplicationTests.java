package br.com.cmms.cmms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies the Spring application context boots with the
 * default (dev) profile against the in-memory H2 database. Catches
 * configuration regressions before they reach a real environment.
 */
@SpringBootTest
@TestPropertySource(properties = {
    // Deterministic 64-byte secret for the test context. Never used outside tests.
    "JWT_SECRET=test-secret-32bytes-or-longer-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    // Disable scheduled keep-alive ping during tests.
    "app.self.url=http://localhost"
})
class CmmsApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: Spring will fail the test if the application
        // context cannot be created.
    }
}
