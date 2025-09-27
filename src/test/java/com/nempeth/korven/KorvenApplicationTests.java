package com.nempeth.korven;

import com.nempeth.korven.config.TestMailConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the Korven application.
 * This test class verifies that the Spring Boot application context loads correctly
 * with the test profile enabled.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfiguration.class)
class KorvenApplicationTests {

    /**
     * Test to verify that the Spring application context loads successfully.
     * This is a basic smoke test that ensures all beans can be instantiated
     * and configured without errors in the test environment.
     */
    @Test
    void contextLoads() {
        // Test passes if application context loads without exceptions
    }
}