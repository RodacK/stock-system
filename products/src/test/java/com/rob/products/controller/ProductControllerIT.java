package com.rob.products.controller;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Sql(statements = "DELETE FROM products", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProductControllerIT {

    private static final String JSON_API = "application/vnd.api+json";
    private static final String ADMIN    = "Bearer admin-key";
    private static final String READ     = "Bearer read-only-key";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("PRODUCTS_DB_URL", postgres::getJdbcUrl);
        registry.add("PRODUCTS_DB_USERNAME", postgres::getUsername);
        registry.add("PRODUCTS_DB_PASSWORD", postgres::getPassword);
    }

    @Autowired WebApplicationContext wac;
    @Autowired Filter springSecurityFilterChain;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac)
                .addFilter(springSecurityFilterChain)
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /v1/products
    // -------------------------------------------------------------------------

    @Test
    void create_returns201_withProductData() throws Exception {
        mvc.perform(post("/v1/products")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(createBody("Widget", "9.99", "A great widget")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.attributes.name").value("Widget"))
                .andExpect(jsonPath("$.data.attributes.price").value(9.99))
                .andExpect(jsonPath("$.data.attributes.description").value("A great widget"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void create_returns409_whenNameAlreadyExists() throws Exception {
        mvc.perform(post("/v1/products")
                .contentType(JSON_API).header("Authorization", ADMIN)
                .content(createBody("Widget", "9.99", null)));

        mvc.perform(post("/v1/products")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(createBody("Widget", "5.00", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].status").value("409"));
    }

    @Test
    void create_returns400_whenNameIsTooShort() throws Exception {
        mvc.perform(post("/v1/products")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(createBody("Ab", "9.99", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].status").value("400"));
    }

    @Test
    void create_returns400_whenPriceIsNull() throws Exception {
        String body = """
                {"data":{"type":"products","attributes":{"name":"ValidName","price":null}}}
                """;
        mvc.perform(post("/v1/products")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns403_withReadOnlyKey() throws Exception {
        mvc.perform(post("/v1/products")
                        .contentType(JSON_API)
                        .header("Authorization", READ)
                        .content(createBody("Widget", "9.99", null)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /v1/products
    // -------------------------------------------------------------------------

    @Test
    void findAll_returns200_withEmptyList_whenNoProducts() throws Exception {
        mvc.perform(get("/v1/products").header("Authorization", READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.totalRecords").value(0));
    }

    @Test
    void findAll_returns200_withProducts() throws Exception {
        mvc.perform(post("/v1/products")
                .contentType(JSON_API).header("Authorization", ADMIN)
                .content(createBody("Widget A", "10.00", null)));
        mvc.perform(post("/v1/products")
                .contentType(JSON_API).header("Authorization", ADMIN)
                .content(createBody("Widget B", "20.00", null)));

        mvc.perform(get("/v1/products").header("Authorization", READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.meta.totalRecords").value(2));
    }

    @Test
    void findAll_returns403_withoutAuth() throws Exception {
        mvc.perform(get("/v1/products"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /v1/products/{id}
    // -------------------------------------------------------------------------

    @Test
    void findById_returns200_withProductData() throws Exception {
        String createResponse = mvc.perform(post("/v1/products")
                        .contentType(JSON_API).header("Authorization", ADMIN)
                        .content(createBody("Widget", "9.99", "desc")))
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

        mvc.perform(get("/v1/products/" + id).header("Authorization", READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.name").value("Widget"))
                .andExpect(jsonPath("$.data.attributes.price").value(9.99));
    }

    @Test
    void findById_returns404_whenProductNotFound() throws Exception {
        mvc.perform(get("/v1/products/999").header("Authorization", READ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String createBody(String name, String price, String description) {
        String descField = description != null
                ? "\"description\":\"" + description + "\""
                : "\"description\":null";
        return """
                {"data":{"type":"products","attributes":{"name":"%s","price":%s,%s}}}
                """.formatted(name, price, descField);
    }
}