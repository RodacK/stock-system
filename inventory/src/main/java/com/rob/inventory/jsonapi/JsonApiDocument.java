package com.rob.inventory.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiDocument<D> {
    @Valid
    private D data;
    private JsonApiMeta meta;
    private JsonApiLinks links;
}