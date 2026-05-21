package com.rob.inventory.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiLinks {
    private String self;
    private String first;
    private String last;
    private String prev;
    private String next;
}