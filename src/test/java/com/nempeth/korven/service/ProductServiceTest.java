package com.nempeth.korven.service;

import com.nempeth.korven.constants.Role;
import com.nempeth.korven.persistence.entity.Product;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.ProductRepository;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.ProductResponse;
import com.nempeth.korven.rest.dto.ProductUpsertRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    private User ownerUser;
    private User nonOwnerUser;
    private Product testProduct;
    private ProductUpsertRequest testRequest;
    private final UUID OWNER_ID = UUID.randomUUID();
    private final UUID NON_OWNER_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ownerUser = User.builder()
                .id(OWNER_ID)
                .email("owner@example.com")
                .name("Owner")
                .lastName("User")
                .passwordHash("hashedPassword")
                .role(Role.OWNER)
                .build();

        nonOwnerUser = User.builder()
                .id(NON_OWNER_ID)
                .email("user@example.com")
                .name("Regular")
                .lastName("User")
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        testProduct = Product.builder()
                .id(PRODUCT_ID)
                .owner(ownerUser)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("29.99"))
                .build();

        testRequest = new ProductUpsertRequest(
                "Test Product",
                "Test Description",
                new BigDecimal("29.99")
        );
    }

    @Test
    void create_withValidOwnerAndUniqueProduct_shouldCreateProduct() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(productRepository.existsByOwnerIdAndNameIgnoreCase(OWNER_ID, testRequest.name())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(PRODUCT_ID);
            return product;
        });

        UUID result = productService.create(OWNER_ID, testRequest);
        assertEquals(PRODUCT_ID, result);
        verify(userRepository).findById(OWNER_ID);
        verify(productRepository).existsByOwnerIdAndNameIgnoreCase(OWNER_ID, testRequest.name());
        verify(productRepository).save(argThat(product -> 
            product.getOwner().equals(ownerUser) &&
            product.getName().equals(testRequest.name()) &&
            product.getDescription().equals(testRequest.description()) &&
            product.getPrice().equals(testRequest.price())
        ));
    }

    @Test
    void create_withNonExistentOwner_shouldThrowException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.create(nonExistentId, testRequest));

        assertEquals("Dueño no encontrado", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
        verify(productRepository, never()).existsByOwnerIdAndNameIgnoreCase(any(), any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_withNonOwnerUser_shouldThrowException() {
        when(userRepository.findById(NON_OWNER_ID)).thenReturn(Optional.of(nonOwnerUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.create(NON_OWNER_ID, testRequest));

        assertEquals("El usuario no es dueño", exception.getMessage());
        verify(userRepository).findById(NON_OWNER_ID);
        verify(productRepository, never()).existsByOwnerIdAndNameIgnoreCase(any(), any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_withDuplicateProductName_shouldThrowException() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(productRepository.existsByOwnerIdAndNameIgnoreCase(OWNER_ID, testRequest.name())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.create(OWNER_ID, testRequest));

        assertEquals("Ya existe un producto con ese nombre para este dueño", exception.getMessage());
        verify(userRepository).findById(OWNER_ID);
        verify(productRepository).existsByOwnerIdAndNameIgnoreCase(OWNER_ID, testRequest.name());
        verify(productRepository, never()).save(any());
    }

    @Test
    void listByOwner_withValidOwner_shouldReturnProductList() {
        Product product2 = Product.builder()
                .id(UUID.randomUUID())
                .owner(ownerUser)
                .name("Product 2")
                .description("Description 2")
                .price(new BigDecimal("19.99"))
                .build();

        List<Product> products = Arrays.asList(testProduct, product2);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(productRepository.findByOwner(ownerUser)).thenReturn(products);

        List<ProductResponse> result = productService.listByOwner(OWNER_ID);
        assertEquals(2, result.size());
        
        ProductResponse response1 = result.get(0);
        assertEquals(testProduct.getId(), response1.id());
        assertEquals(testProduct.getName(), response1.name());
        assertEquals(testProduct.getDescription(), response1.description());
        assertEquals(testProduct.getPrice(), response1.price());

        ProductResponse response2 = result.get(1);
        assertEquals(product2.getId(), response2.id());
        assertEquals(product2.getName(), response2.name());
        assertEquals(product2.getDescription(), response2.description());
        assertEquals(product2.getPrice(), response2.price());

        verify(userRepository).findById(OWNER_ID);
        verify(productRepository).findByOwner(ownerUser);
    }

    @Test
    void listByOwner_withNonExistentOwner_shouldThrowException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.listByOwner(nonExistentId));

        assertEquals("Dueño no encontrado", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
        verify(productRepository, never()).findByOwner(any());
    }

    @Test
    void listByOwner_withNonOwnerUser_shouldThrowException() {
        when(userRepository.findById(NON_OWNER_ID)).thenReturn(Optional.of(nonOwnerUser));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.listByOwner(NON_OWNER_ID));

        assertEquals("El usuario no es dueño", exception.getMessage());
        verify(userRepository).findById(NON_OWNER_ID);
        verify(productRepository, never()).findByOwner(any());
    }

    @Test
    void listByOwner_withEmptyProductList_shouldReturnEmptyList() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(productRepository.findByOwner(ownerUser)).thenReturn(Arrays.asList());

        List<ProductResponse> result = productService.listByOwner(OWNER_ID);
        assertTrue(result.isEmpty());
        verify(userRepository).findById(OWNER_ID);
        verify(productRepository).findByOwner(ownerUser);
    }

    @Test
    void update_withValidProductAndOwner_shouldUpdateProduct() {
        ProductUpsertRequest updateRequest = new ProductUpsertRequest(
                "Updated Product",
                "Updated Description",
                new BigDecimal("39.99")
        );
        when(productRepository.findByIdAndOwnerId(PRODUCT_ID, OWNER_ID)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(testProduct)).thenReturn(testProduct);

        productService.update(OWNER_ID, PRODUCT_ID, updateRequest);

        assertEquals(updateRequest.name(), testProduct.getName());
        assertEquals(updateRequest.description(), testProduct.getDescription());
        assertEquals(updateRequest.price(), testProduct.getPrice());
        verify(productRepository).findByIdAndOwnerId(PRODUCT_ID, OWNER_ID);
        verify(productRepository).save(testProduct);
    }

    @Test
    void update_withNonExistentProduct_shouldThrowException() {
        ProductUpsertRequest updateRequest = new ProductUpsertRequest(
                "Updated Product",
                "Updated Description",
                new BigDecimal("39.99")
        );
        when(productRepository.findByIdAndOwnerId(PRODUCT_ID, OWNER_ID)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.update(OWNER_ID, PRODUCT_ID, updateRequest));

        assertEquals("Producto no encontrado para este dueño", exception.getMessage());
        verify(productRepository).findByIdAndOwnerId(PRODUCT_ID, OWNER_ID);
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_withWrongOwner_shouldThrowException() {
        UUID wrongOwnerId = UUID.randomUUID();
        ProductUpsertRequest updateRequest = new ProductUpsertRequest(
                "Updated Product",
                "Updated Description",
                new BigDecimal("39.99")
        );
        when(productRepository.findByIdAndOwnerId(PRODUCT_ID, wrongOwnerId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.update(wrongOwnerId, PRODUCT_ID, updateRequest));

        assertEquals("Producto no encontrado para este dueño", exception.getMessage());
        verify(productRepository).findByIdAndOwnerId(PRODUCT_ID, wrongOwnerId);
        verify(productRepository, never()).save(any());
    }

    @Test
    void delete_withValidProductAndOwner_shouldDeleteProduct() {
        when(productRepository.findByIdAndOwnerId(PRODUCT_ID, OWNER_ID)).thenReturn(Optional.of(testProduct));
        productService.delete(OWNER_ID, PRODUCT_ID);
        verify(productRepository).findByIdAndOwnerId(PRODUCT_ID, OWNER_ID);
        verify(productRepository).delete(testProduct);
    }

    @Test
    void delete_withNonExistentProduct_shouldThrowException() {
        when(productRepository.findByIdAndOwnerId(PRODUCT_ID, OWNER_ID)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.delete(OWNER_ID, PRODUCT_ID));

        assertEquals("Producto no encontrado para este dueño", exception.getMessage());
        verify(productRepository).findByIdAndOwnerId(PRODUCT_ID, OWNER_ID);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void delete_withWrongOwner_shouldThrowException() {
        UUID wrongOwnerId = UUID.randomUUID();
        when(productRepository.findByIdAndOwnerId(PRODUCT_ID, wrongOwnerId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.delete(wrongOwnerId, PRODUCT_ID));

        assertEquals("Producto no encontrado para este dueño", exception.getMessage());
        verify(productRepository).findByIdAndOwnerId(PRODUCT_ID, wrongOwnerId);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void mustFindOwner_withValidOwner_shouldReturnOwner() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(productRepository.existsByOwnerIdAndNameIgnoreCase(OWNER_ID, testRequest.name())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        assertDoesNotThrow(() -> productService.create(OWNER_ID, testRequest));
        verify(userRepository).findById(OWNER_ID);
    }

    @Test
    void create_caseInsensitiveNameCheck_shouldDetectDuplicate() {
        ProductUpsertRequest requestWithDifferentCase = new ProductUpsertRequest(
                "TEST PRODUCT", 
                "Test Description",
                new BigDecimal("29.99")
        );
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(productRepository.existsByOwnerIdAndNameIgnoreCase(OWNER_ID, "TEST PRODUCT")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> productService.create(OWNER_ID, requestWithDifferentCase));

        assertEquals("Ya existe un producto con ese nombre para este dueño", exception.getMessage());
        verify(productRepository).existsByOwnerIdAndNameIgnoreCase(OWNER_ID, "TEST PRODUCT");
    }
}