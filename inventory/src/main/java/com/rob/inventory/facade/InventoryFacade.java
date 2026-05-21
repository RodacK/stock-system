package com.rob.inventory.facade;

import com.rob.inventory.dto.InventoryAttributes;
import com.rob.inventory.dto.InventoryRequest;
import com.rob.inventory.dto.PurchaseAttributes;
import com.rob.inventory.dto.PurchaseRequest;

public interface InventoryFacade {

    boolean exists(Long productId);

    InventoryAttributes upsert(InventoryRequest request);

    InventoryAttributes findByProductId(Long productId);

    PurchaseAttributes purchase(PurchaseRequest request);
}