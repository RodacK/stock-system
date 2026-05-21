package com.rob.inventory.dto;

import java.math.BigDecimal;

public record InventoryAttributes(
        Long productId,
        Integer quantity,
        String productName,
        BigDecimal productPrice
) {}