package com.rob.inventory.client;

import com.rob.inventory.dto.ProductClientResponse;
import com.rob.inventory.exception.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductsClient {

    private final RestClient productsRestClient;

    @Retry(name = "products")
    @CircuitBreaker(name = "products", fallbackMethod = "fallback")
    public ProductClientResponse findById(Long productId) {
        return productsRestClient.get()
                .uri("/v1/products/{id}", productId)
                .retrieve()
                .body(ProductClientResponse.class);
    }

    private ProductClientResponse fallback(Long productId, Throwable ex) {
        if (ex instanceof HttpClientErrorException.NotFound) {
            throw new ProductNotFoundException(productId);
        }
        log.warn("Products service unavailable after retries for productId={}: {}", productId, ex.getMessage());
        return null;
    }
}