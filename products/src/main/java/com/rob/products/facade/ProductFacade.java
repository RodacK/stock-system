package com.rob.products.facade;

import com.rob.products.dto.ProductRequest;
import com.rob.products.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductFacade {

    ProductResponse create(ProductRequest request);

    Page<ProductResponse> findAll(Pageable pageable);

    ProductResponse findById(Long id);
}