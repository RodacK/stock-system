package com.rob.products.service;

import com.rob.products.dto.ProductRequest;
import com.rob.products.dto.ProductResponse;
import com.rob.products.entity.Product;
import com.rob.products.exception.ProductAlreadyExistsException;
import com.rob.products.exception.ProductNotFoundException;
import com.rob.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ProductAlreadyExistsException(request.getName());
        }
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setDescription(product.getDescription());
        return response;
    }
}