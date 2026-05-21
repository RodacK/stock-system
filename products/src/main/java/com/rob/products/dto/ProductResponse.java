package com.rob.products.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String description;
}