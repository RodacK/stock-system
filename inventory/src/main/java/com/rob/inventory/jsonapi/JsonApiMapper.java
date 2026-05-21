package com.rob.inventory.jsonapi;

import org.springframework.data.domain.Page;

import java.util.List;

public interface JsonApiMapper<S, A> {

    String getType();

    String getId(S source);

    A toAttributes(S source);

    default JsonApiData<A> toData(S source) {
        return new JsonApiData<>(getType(), getId(source), toAttributes(source));
    }

    default JsonApiDocument<JsonApiData<A>> toDocument(S source) {
        return new JsonApiDocument<>(toData(source), null, null);
    }

    default JsonApiDocument<List<JsonApiData<A>>> toDocument(Page<S> page) {
        List<JsonApiData<A>> data = page.getContent().stream()
                .map(this::toData)
                .toList();
        JsonApiMeta meta = JsonApiMeta.builder()
                .totalRecords(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .pageSize(page.getSize())
                .build();
        return new JsonApiDocument<>(data, meta, null);
    }
}