package com.rob.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class InventoryApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("INVENTORY_DB_URL", postgres::getJdbcUrl);
        registry.add("INVENTORY_DB_USERNAME", postgres::getUsername);
        registry.add("INVENTORY_DB_PASSWORD", postgres::getPassword);
    }

    @Test
    void contextLoads() {
    }
}