package com.rob.products;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ProductsApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("PRODUCTS_DB_URL", postgres::getJdbcUrl);
        registry.add("PRODUCTS_DB_USERNAME", postgres::getUsername);
        registry.add("PRODUCTS_DB_PASSWORD", postgres::getPassword);
    }

    @Test
    void contextLoads() {
    }
}