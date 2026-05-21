package com.rob.inventory.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiErrorSource {
    private String pointer;
    private String parameter;
}