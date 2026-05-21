package com.rob.products.jsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsonApiMeta {
    @JsonProperty("total-records")
    private long totalRecords;
    @JsonProperty("total-pages")
    private int totalPages;
    private int page;
    @JsonProperty("page-size")
    private int pageSize;
}