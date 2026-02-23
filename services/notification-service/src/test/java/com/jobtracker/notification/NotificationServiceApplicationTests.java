package com.jobtracker.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 * Requires a running MySQL instance and RabbitMQ broker, or test-profile overrides
 * for those infrastructure beans. Wire up Testcontainers if full integration coverage
 * is needed in CI.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
        // passes if the application context starts without throwing
    }
}
