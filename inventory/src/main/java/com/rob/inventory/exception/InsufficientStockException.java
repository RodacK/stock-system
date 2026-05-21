package com.rob.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super("Insufficient stock for productId: " + productId
                + ". Requested: " + requested + ", available: " + available);
    }
}