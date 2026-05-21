package com.rob.inventory.service;

import com.rob.inventory.client.ProductsClient;
import com.rob.inventory.dto.InventoryAttributes;
import com.rob.inventory.dto.InventoryRequest;
import com.rob.inventory.dto.ProductClientResponse;
import com.rob.inventory.dto.PurchaseAttributes;
import com.rob.inventory.dto.PurchaseRequest;
import com.rob.inventory.entity.Inventory;
import com.rob.inventory.exception.InsufficientStockException;
import com.rob.inventory.exception.InventoryNotFoundException;
import com.rob.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductsClient productsClient;

    public boolean exists(Long productId) {
        return inventoryRepository.existsById(productId);
    }

    @Transactional
    public InventoryAttributes upsert(InventoryRequest request) {
        ProductClientResponse product = productsClient.findById(request.getProductId());
        Inventory inventory = inventoryRepository.findById(request.getProductId())
                .map(existing -> {
                    existing.setQuantity(request.getQuantity());
                    return existing;
                })
                .orElse(new Inventory(request.getProductId(), request.getQuantity()));
        inventoryRepository.save(inventory);
        return toAttributes(inventory, product);
    }

    @Transactional(readOnly = true)
    public InventoryAttributes findByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
        return toAttributes(inventory, productsClient.findById(productId));
    }

    @Transactional
    public PurchaseAttributes purchase(PurchaseRequest request) {
        Inventory inventory = inventoryRepository.findByIdWithLock(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(request.getProductId()));

        if (inventory.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(request.getProductId(), request.getQuantity(), inventory.getQuantity());
        }

        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        inventoryRepository.save(inventory);

        ProductClientResponse product = productsClient.findById(request.getProductId());
        BigDecimal price = product != null ? product.getPrice() : null;
        BigDecimal total = price != null ? price.multiply(BigDecimal.valueOf(request.getQuantity())) : null;

        return new PurchaseAttributes(
                request.getProductId(),
                request.getQuantity(),
                inventory.getQuantity(),
                product != null ? product.getName() : null,
                price,
                total
        );
    }

    private InventoryAttributes toAttributes(Inventory inventory, ProductClientResponse product) {
        String name = product != null ? product.getName() : null;
        BigDecimal price = product != null ? product.getPrice() : null;
        return new InventoryAttributes(inventory.getProductId(), inventory.getQuantity(), name, price);
    }
}