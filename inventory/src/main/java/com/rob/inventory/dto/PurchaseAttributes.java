package com.rob.inventory.dto;

import java.math.BigDecimal;

public record PurchaseAttributes(
        Long productId,
        Integer quantityPurchased,
        Integer remainingQuantity,
        String productName,
        BigDecimal productPrice,
        BigDecimal totalPrice
) {}