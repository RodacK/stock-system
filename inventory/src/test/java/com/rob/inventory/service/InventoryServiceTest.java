package com.rob.inventory.service;

import com.rob.inventory.dto.InventoryAttributes;
import com.rob.inventory.dto.InventoryRequest;
import com.rob.inventory.dto.ProductClientResponse;
import com.rob.inventory.dto.PurchaseAttributes;
import com.rob.inventory.dto.PurchaseRequest;
import com.rob.inventory.entity.Inventory;
import com.rob.inventory.exception.InsufficientStockException;
import com.rob.inventory.exception.InventoryNotFoundException;
import com.rob.inventory.exception.ProductNotFoundException;
import com.rob.inventory.client.ProductsClient;
import com.rob.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock ProductsClient productsClient;
    @InjectMocks InventoryService inventoryService;

    // -------------------------------------------------------------------------
    // exists()
    // -------------------------------------------------------------------------

    @Test
    void exists_returnsTrue_whenInventoryRecordExists() {
        when(inventoryRepository.existsById(1L)).thenReturn(true);

        assertThat(inventoryService.exists(1L)).isTrue();
    }

    @Test
    void exists_returnsFalse_whenNoInventoryRecord() {
        when(inventoryRepository.existsById(99L)).thenReturn(false);

        assertThat(inventoryService.exists(99L)).isFalse();
    }

    // -------------------------------------------------------------------------
    // upsert()
    // -------------------------------------------------------------------------

    @Test
    void upsert_savesNewRecord_whenProductHasNoInventory() {
        InventoryRequest request = request(1L, 50);
        ProductClientResponse product = productResponse("Widget", BigDecimal.valueOf(9.99));

        when(productsClient.findById(1L)).thenReturn(product);
        when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryAttributes result = inventoryService.upsert(request);

        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.quantity()).isEqualTo(50);
        assertThat(result.productName()).isEqualTo("Widget");
        assertThat(result.productPrice()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void upsert_updatesQuantity_whenInventoryAlreadyExists() {
        InventoryRequest request = request(1L, 100);
        Inventory existing = new Inventory(1L, 30);

        when(productsClient.findById(1L)).thenReturn(null);
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryAttributes result = inventoryService.upsert(request);

        assertThat(result.quantity()).isEqualTo(100);
        assertThat(existing.getQuantity()).isEqualTo(100);
    }

    @Test
    void upsert_throwsProductNotFoundException_whenProductDoesNotExist() {
        InventoryRequest request = request(99L, 10);

        when(productsClient.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        assertThatThrownBy(() -> inventoryService.upsert(request))
                .isInstanceOf(ProductNotFoundException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void upsert_proceedsWithNullProductData_whenProductServiceUnavailable() {
        InventoryRequest request = request(1L, 20);

        when(productsClient.findById(1L)).thenReturn(null);
        when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryAttributes result = inventoryService.upsert(request);

        assertThat(result.quantity()).isEqualTo(20);
        assertThat(result.productName()).isNull();
        assertThat(result.productPrice()).isNull();
    }

    // -------------------------------------------------------------------------
    // findByProductId()
    // -------------------------------------------------------------------------

    @Test
    void findByProductId_returnsAttributes_whenInventoryExists() {
        Inventory inventory = new Inventory(1L, 20);
        ProductClientResponse product = productResponse("Widget", BigDecimal.valueOf(5.00));

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(productsClient.findById(1L)).thenReturn(product);

        InventoryAttributes result = inventoryService.findByProductId(1L);

        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.quantity()).isEqualTo(20);
        assertThat(result.productName()).isEqualTo("Widget");
        assertThat(result.productPrice()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
    }

    @Test
    void findByProductId_throwsInventoryNotFoundException_whenNoRecord() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.findByProductId(99L))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // purchase()
    // -------------------------------------------------------------------------

    @Test
    void purchase_deductsStockAndReturnsSummary_onSuccess() {
        PurchaseRequest request = purchaseRequest(1L, 5);
        Inventory inventory = new Inventory(1L, 20);
        ProductClientResponse product = productResponse("Widget", BigDecimal.valueOf(10.00));

        when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productsClient.findById(1L)).thenReturn(product);

        PurchaseAttributes result = inventoryService.purchase(request);

        assertThat(result.quantityPurchased()).isEqualTo(5);
        assertThat(result.remainingQuantity()).isEqualTo(15);
        assertThat(result.productName()).isEqualTo("Widget");
        assertThat(result.productPrice()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(result.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(inventory.getQuantity()).isEqualTo(15);
    }

    @Test
    void purchase_throwsInventoryNotFoundException_whenProductNotInInventory() {
        PurchaseRequest request = purchaseRequest(99L, 1);

        when(inventoryRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.purchase(request))
                .isInstanceOf(InventoryNotFoundException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void purchase_throwsInsufficientStockException_whenRequestedQuantityExceedsStock() {
        PurchaseRequest request = purchaseRequest(1L, 50);
        Inventory inventory = new Inventory(1L, 10);

        when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.purchase(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(inventoryRepository, never()).save(any());
        assertThat(inventory.getQuantity()).isEqualTo(10);
    }

    @Test
    void purchase_leavesNullPriceFields_whenProductServiceUnavailable() {
        PurchaseRequest request = purchaseRequest(1L, 3);
        Inventory inventory = new Inventory(1L, 10);

        when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productsClient.findById(1L)).thenReturn(null);

        PurchaseAttributes result = inventoryService.purchase(request);

        assertThat(result.remainingQuantity()).isEqualTo(7);
        assertThat(result.productName()).isNull();
        assertThat(result.productPrice()).isNull();
        assertThat(result.totalPrice()).isNull();
    }

    @Test
    void purchase_exactlyDrainsStock_whenQuantityEqualsAvailable() {
        PurchaseRequest request = purchaseRequest(1L, 10);
        Inventory inventory = new Inventory(1L, 10);

        when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productsClient.findById(1L)).thenReturn(null);

        PurchaseAttributes result = inventoryService.purchase(request);

        assertThat(result.remainingQuantity()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private InventoryRequest request(Long productId, int quantity) {
        InventoryRequest r = new InventoryRequest();
        r.setProductId(productId);
        r.setQuantity(quantity);
        return r;
    }

    private PurchaseRequest purchaseRequest(Long productId, int quantity) {
        PurchaseRequest r = new PurchaseRequest();
        r.setProductId(productId);
        r.setQuantity(quantity);
        return r;
    }

    private ProductClientResponse productResponse(String name, BigDecimal price) {
        ProductClientResponse response = new ProductClientResponse();
        ProductClientResponse.DataWrapper data = new ProductClientResponse.DataWrapper();
        ProductClientResponse.Attributes attributes = new ProductClientResponse.Attributes();
        attributes.setName(name);
        attributes.setPrice(price);
        data.setAttributes(attributes);
        data.setId("1");
        response.setData(data);
        return response;
    }
}