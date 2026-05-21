package com.rob.products.service;

import com.rob.products.dto.ProductRequest;
import com.rob.products.dto.ProductResponse;
import com.rob.products.entity.Product;
import com.rob.products.exception.ProductAlreadyExistsException;
import com.rob.products.exception.ProductNotFoundException;
import com.rob.products.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductService productService;

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Test
    void create_savesAndReturnsProduct_whenNameIsUnique() {
        ProductRequest request = request("Widget Pro", BigDecimal.valueOf(19.99), "A great widget");

        when(productRepository.existsByNameIgnoreCase("Widget Pro")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse result = productService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Widget Pro");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(19.99));
        assertThat(result.getDescription()).isEqualTo("A great widget");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_throwsProductAlreadyExistsException_whenNameExists() {
        ProductRequest request = request("Duplicate", BigDecimal.ONE, null);

        when(productRepository.existsByNameIgnoreCase("Duplicate")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ProductAlreadyExistsException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void create_mapsNullDescription_whenNotProvided() {
        ProductRequest request = request("Minimal", BigDecimal.valueOf(5.00), null);

        when(productRepository.existsByNameIgnoreCase("Minimal")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        ProductResponse result = productService.create(request);

        assertThat(result.getDescription()).isNull();
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Test
    void findAll_returnsMappedPage_whenProductsExist() {
        Product product = new Product(1L, "Widget", BigDecimal.valueOf(9.99), "desc");

        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> result = productService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Widget");
    }

    @Test
    void findAll_returnsEmptyPage_whenNoProducts() {
        when(productRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        Page<ProductResponse> result = productService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------

    @Test
    void findById_returnsProduct_whenExists() {
        Product product = new Product(1L, "Widget", BigDecimal.valueOf(9.99), null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Widget");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
        assertThat(result.getDescription()).isNull();
    }

    @Test
    void findById_throwsProductNotFoundException_whenNotExists() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProductRequest request(String name, BigDecimal price, String description) {
        ProductRequest r = new ProductRequest();
        r.setName(name);
        r.setPrice(price);
        r.setDescription(description);
        return r;
    }
}