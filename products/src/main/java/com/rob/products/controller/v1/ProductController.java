package com.rob.products.controller.v1;

import com.rob.products.dto.ProductAttributes;
import com.rob.products.dto.ProductRequest;
import com.rob.products.dto.ProductResponse;
import com.rob.products.facade.ProductFacade;
import com.rob.products.jsonapi.JsonApiData;
import com.rob.products.jsonapi.JsonApiDocument;
import com.rob.products.jsonapi.JsonApiMeta;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/products", produces = "application/vnd.api+json")
@RequiredArgsConstructor
public class ProductController {

    private static final String TYPE = "products";

    private final ProductFacade productFacade;

    @PostMapping(consumes = "application/vnd.api+json")
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimiter(name = "products-write")
    public JsonApiDocument<JsonApiData<ProductAttributes>> create(
            @Valid @RequestBody JsonApiDocument<JsonApiData<ProductRequest>> body) {
        return new JsonApiDocument<>(toData(productFacade.create(body.getData().getAttributes())), null, null);
    }

    @GetMapping
    @RateLimiter(name = "products-read")
    public JsonApiDocument<List<JsonApiData<ProductAttributes>>> findAll(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ProductResponse> page = productFacade.findAll(pageable);
        List<JsonApiData<ProductAttributes>> data = page.getContent().stream()
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

    @GetMapping("/{id}")
    @RateLimiter(name = "products-read")
    public JsonApiDocument<JsonApiData<ProductAttributes>> findById(@PathVariable Long id) {
        return new JsonApiDocument<>(toData(productFacade.findById(id)), null, null);
    }

    private JsonApiData<ProductAttributes> toData(ProductResponse product) {
        return new JsonApiData<>(TYPE, String.valueOf(product.getId()),
                new ProductAttributes(product.getName(), product.getPrice(), product.getDescription()));
    }
}