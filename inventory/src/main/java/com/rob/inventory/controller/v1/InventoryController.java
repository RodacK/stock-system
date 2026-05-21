package com.rob.inventory.controller.v1;

import com.rob.inventory.dto.InventoryAttributes;
import com.rob.inventory.dto.InventoryRequest;
import com.rob.inventory.dto.PurchaseAttributes;
import com.rob.inventory.dto.PurchaseRequest;
import com.rob.inventory.facade.InventoryFacade;
import com.rob.inventory.jsonapi.JsonApiData;
import com.rob.inventory.jsonapi.JsonApiDocument;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/inventory", produces = "application/vnd.api+json")
@RequiredArgsConstructor
public class InventoryController {

    private static final String TYPE = "inventory";

    private final InventoryFacade inventoryFacade;

    @PostMapping(consumes = "application/vnd.api+json")
    @RateLimiter(name = "inventory-write")
    public ResponseEntity<JsonApiDocument<JsonApiData<InventoryAttributes>>> upsert(
            @Valid @RequestBody JsonApiDocument<JsonApiData<InventoryRequest>> body) {
        boolean existed = inventoryFacade.exists(body.getData().getAttributes().getProductId());
        InventoryAttributes attrs = inventoryFacade.upsert(body.getData().getAttributes());
        HttpStatus status = existed ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(new JsonApiDocument<>(toData(attrs), null, null));
    }

    @GetMapping("/{productId}")
    @RateLimiter(name = "inventory-read")
    public JsonApiDocument<JsonApiData<InventoryAttributes>> findByProductId(@PathVariable Long productId) {
        InventoryAttributes attrs = inventoryFacade.findByProductId(productId);
        return new JsonApiDocument<>(toData(attrs), null, null);
    }

    @PostMapping(value = "/purchase", consumes = "application/vnd.api+json")
    @RateLimiter(name = "inventory-write")
    public JsonApiDocument<JsonApiData<PurchaseAttributes>> purchase(
            @Valid @RequestBody JsonApiDocument<JsonApiData<PurchaseRequest>> body) {
        PurchaseAttributes attrs = inventoryFacade.purchase(body.getData().getAttributes());
        return new JsonApiDocument<>(
                new JsonApiData<>("purchases", String.valueOf(attrs.productId()), attrs), null, null);
    }

    private JsonApiData<InventoryAttributes> toData(InventoryAttributes attrs) {
        return new JsonApiData<>(TYPE, String.valueOf(attrs.productId()), attrs);
    }
}