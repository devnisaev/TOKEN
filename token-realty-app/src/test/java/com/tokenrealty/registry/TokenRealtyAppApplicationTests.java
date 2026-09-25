package com.tokenrealty.registry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // uses H2, no real PostgreSQL needed
class TokenRealtyAppApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring context assembles without errors
    }
}