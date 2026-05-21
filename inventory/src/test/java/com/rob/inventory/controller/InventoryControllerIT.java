package com.rob.inventory.controller;

import com.rob.inventory.client.ProductsClient;
import com.rob.inventory.dto.ProductClientResponse;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Sql(statements = "DELETE FROM inventory", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class InventoryControllerIT {

    private static final String JSON_API = "application/vnd.api+json";
    private static final String ADMIN    = "Bearer admin-key";
    private static final String READ     = "Bearer read-only-key";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("INVENTORY_DB_URL", postgres::getJdbcUrl);
        registry.add("INVENTORY_DB_USERNAME", postgres::getUsername);
        registry.add("INVENTORY_DB_PASSWORD", postgres::getPassword);
    }

    @Autowired WebApplicationContext wac;
    @Autowired Filter springSecurityFilterChain;

    @MockitoBean ProductsClient productsClient;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac)
                .addFilter(springSecurityFilterChain)
                .build();
        when(productsClient.findById(anyLong()))
                .thenReturn(buildProduct("Widget", new BigDecimal("10.00")));
    }

    // -------------------------------------------------------------------------
    // POST /v1/inventory (upsert)
    // -------------------------------------------------------------------------

    @Test
    void upsert_returns201_whenNewProduct() throws Exception {
        mvc.perform(post("/v1/inventory")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(upsertBody(1L, 50)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.attributes.productId").value(1))
                .andExpect(jsonPath("$.data.attributes.quantity").value(50))
                .andExpect(jsonPath("$.data.attributes.productName").value("Widget"));
    }

    @Test
    void upsert_returns200_andUpdatesQuantity_whenInventoryExists() throws Exception {
        mvc.perform(post("/v1/inventory")
                .contentType(JSON_API).header("Authorization", ADMIN).content(upsertBody(1L, 50)));

        mvc.perform(post("/v1/inventory")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(upsertBody(1L, 100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.quantity").value(100));
    }

    @Test
    void upsert_returns403_withReadOnlyKey() throws Exception {
        mvc.perform(post("/v1/inventory")
                        .contentType(JSON_API)
                        .header("Authorization", READ)
                        .content(upsertBody(1L, 50)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /v1/inventory/{productId}
    // -------------------------------------------------------------------------

    @Test
    void findByProductId_returns200_withInventoryData() throws Exception {
        mvc.perform(post("/v1/inventory")
                .contentType(JSON_API).header("Authorization", ADMIN).content(upsertBody(1L, 30)));

        mvc.perform(get("/v1/inventory/1").header("Authorization", READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.productId").value(1))
                .andExpect(jsonPath("$.data.attributes.quantity").value(30))
                .andExpect(jsonPath("$.data.attributes.productName").value("Widget"));
    }

    @Test
    void findByProductId_returns404_whenNotInInventory() throws Exception {
        mvc.perform(get("/v1/inventory/999").header("Authorization", READ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"));
    }

    @Test
    void findByProductId_returns403_withoutAuth() throws Exception {
        mvc.perform(get("/v1/inventory/1"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // POST /v1/inventory/purchase
    // -------------------------------------------------------------------------

    @Test
    void purchase_returns200_andDeductsStock() throws Exception {
        mvc.perform(post("/v1/inventory")
                .contentType(JSON_API).header("Authorization", ADMIN).content(upsertBody(1L, 20)));

        mvc.perform(post("/v1/inventory/purchase")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(purchaseBody(1L, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.quantityPurchased").value(5))
                .andExpect(jsonPath("$.data.attributes.remainingQuantity").value(15))
                .andExpect(jsonPath("$.data.attributes.totalPrice").value(50.00));
    }

    @Test
    void purchase_returns422_whenInsufficientStock() throws Exception {
        mvc.perform(post("/v1/inventory")
                .contentType(JSON_API).header("Authorization", ADMIN).content(upsertBody(1L, 5)));

        mvc.perform(post("/v1/inventory/purchase")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(purchaseBody(1L, 10)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errors[0].status").value("422"));
    }

    @Test
    void purchase_returns404_whenProductNotInInventory() throws Exception {
        mvc.perform(post("/v1/inventory/purchase")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(purchaseBody(999L, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"));
    }

    @Test
    void purchase_exactlyDrainsStock_returns200() throws Exception {
        mvc.perform(post("/v1/inventory")
                .contentType(JSON_API).header("Authorization", ADMIN).content(upsertBody(1L, 10)));

        mvc.perform(post("/v1/inventory/purchase")
                        .contentType(JSON_API)
                        .header("Authorization", ADMIN)
                        .content(purchaseBody(1L, 10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.remainingQuantity").value(0));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String upsertBody(long productId, int quantity) {
        return """
                {"data":{"type":"inventory","attributes":{"productId":%d,"quantity":%d}}}
                """.formatted(productId, quantity);
    }

    private String purchaseBody(long productId, int quantity) {
        return """
                {"data":{"type":"purchases","attributes":{"productId":%d,"quantity":%d}}}
                """.formatted(productId, quantity);
    }

    private ProductClientResponse buildProduct(String name, BigDecimal price) {
        ProductClientResponse response = new ProductClientResponse();
        ProductClientResponse.DataWrapper data = new ProductClientResponse.DataWrapper();
        ProductClientResponse.Attributes attributes = new ProductClientResponse.Attributes();
        attributes.setName(name);
        attributes.setPrice(price);
        data.setAttributes(attributes);
        data.setId("1");
        response.setData(data);
        return response;
    }
}