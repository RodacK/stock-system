package com.rob.inventory.jsonapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JsonApiErrorDocument {
    private List<JsonApiError> errors;
}