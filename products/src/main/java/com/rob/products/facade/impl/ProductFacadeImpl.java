package com.rob.products.facade.impl;

import com.rob.products.dto.ProductRequest;
import com.rob.products.dto.ProductResponse;
import com.rob.products.facade.ProductFacade;
import com.rob.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductFacadeImpl implements ProductFacade {

    private final ProductService productService;

    @Override
    public ProductResponse create(ProductRequest request) {
        return productService.create(request);
    }

    @Override
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productService.findAll(pageable);
    }

    @Override
    public ProductResponse findById(Long id) {
        return productService.findById(id);
    }
}