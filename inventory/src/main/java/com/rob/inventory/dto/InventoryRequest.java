package com.rob.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Min(0)
    private Integer quantity;
}