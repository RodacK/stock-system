package com.rob.products.repository;

import com.rob.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameIgnoreCase(String name);
}