package com.rob.inventory.facade.impl;

import com.rob.inventory.dto.InventoryAttributes;
import com.rob.inventory.dto.InventoryRequest;
import com.rob.inventory.dto.PurchaseAttributes;
import com.rob.inventory.dto.PurchaseRequest;
import com.rob.inventory.facade.InventoryFacade;
import com.rob.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryFacadeImpl implements InventoryFacade {

    private final InventoryService inventoryService;

    @Override
    public boolean exists(Long productId) {
        return inventoryService.exists(productId);
    }

    @Override
    public InventoryAttributes upsert(InventoryRequest request) {
        return inventoryService.upsert(request);
    }

    @Override
    public InventoryAttributes findByProductId(Long productId) {
        return inventoryService.findByProductId(productId);
    }

    @Override
    public PurchaseAttributes purchase(PurchaseRequest request) {
        return inventoryService.purchase(request);
    }
}