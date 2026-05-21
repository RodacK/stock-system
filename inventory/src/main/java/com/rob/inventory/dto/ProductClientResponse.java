package com.rob.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductClientResponse {

    private DataWrapper data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataWrapper {
        private String id;
        private Attributes attributes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attributes {
        private String name;
        private BigDecimal price;
        private String description;
    }

    public String getProductId() {
        return data != null ? data.getId() : null;
    }

    public String getName() {
        return data != null && data.getAttributes() != null ? data.getAttributes().getName() : null;
    }

    public BigDecimal getPrice() {
        return data != null && data.getAttributes() != null ? data.getAttributes().getPrice() : null;
    }
}