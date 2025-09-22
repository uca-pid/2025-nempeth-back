package com.nempeth.korven.persistence.repository;

import com.nempeth.korven.config.TestMailConfiguration;
import com.nempeth.korven.constants.Role;
import com.nempeth.korven.persistence.entity.Product;
import com.nempeth.korven.persistence.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestMailConfiguration.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner1;
    private User owner2;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        owner1 = User.builder()
                .email("owner1@example.com")
                .name("John")
                .lastName("Doe")
                .passwordHash("hash123")
                .role(Role.OWNER)
                .build();
        owner1 = userRepository.save(owner1);

        owner2 = User.builder()
                .email("owner2@example.com")
                .name("Jane")
                .lastName("Smith")
                .passwordHash("hash456")
                .role(Role.OWNER)
                .build();
        owner2 = userRepository.save(owner2);

        testProduct = Product.builder()
                .owner(owner1)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("29.99"))
                .build();
    }

    @Test
    void shouldSaveAndFindProductById() {
        Product savedProduct = productRepository.save(testProduct);
        Optional<Product> foundProduct = productRepository.findById(savedProduct.getId());
        
        assertTrue(foundProduct.isPresent());
        assertEquals(savedProduct.getId(), foundProduct.get().getId());
        assertEquals("Test Product", foundProduct.get().getName());
        assertEquals("Test Description", foundProduct.get().getDescription());
        assertEquals(0, new BigDecimal("29.99").compareTo(foundProduct.get().getPrice()));
        assertEquals(owner1.getId(), foundProduct.get().getOwner().getId());
    }

    @Test
    void shouldGenerateUuidOnSave() {
        assertNull(testProduct.getId());
        Product savedProduct = productRepository.save(testProduct);
        assertNotNull(savedProduct.getId());
        assertTrue(savedProduct.getId() instanceof UUID);
    }

    @Test
    void shouldFindProductsByOwner() {
        Product product1 = Product.builder()
                .owner(owner1)
                .name("Product 1")
                .price(new BigDecimal("19.99"))
                .build();
        
        Product product2 = Product.builder()
                .owner(owner1)
                .name("Product 2")
                .price(new BigDecimal("39.99"))
                .build();
        
        Product product3 = Product.builder()
                .owner(owner2)
                .name("Product 3")
                .price(new BigDecimal("49.99"))
                .build();
        
        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);
        List<Product> owner1Products = productRepository.findByOwner(owner1);
        List<Product> owner2Products = productRepository.findByOwner(owner2);
        
        assertEquals(2, owner1Products.size());
        assertEquals(1, owner2Products.size());    
        assertTrue(owner1Products.stream().anyMatch(p -> "Product 1".equals(p.getName())));
        assertTrue(owner1Products.stream().anyMatch(p -> "Product 2".equals(p.getName())));
        assertTrue(owner2Products.stream().anyMatch(p -> "Product 3".equals(p.getName())));
    }

    @Test
    void shouldReturnEmptyListWhenOwnerHasNoProducts() {
        User ownerWithNoProducts = User.builder()
                .email("noproducts@example.com")
                .passwordHash("hash789")
                .role(Role.OWNER)
                .build();
        ownerWithNoProducts = userRepository.save(ownerWithNoProducts);
        List<Product> products = productRepository.findByOwner(ownerWithNoProducts);
        
        assertTrue(products.isEmpty());
    }

    @Test
    void shouldFindProductByIdAndOwnerId() {
        Product savedProduct = productRepository.save(testProduct);
        
        Optional<Product> foundProduct = productRepository.findByIdAndOwnerId(
                savedProduct.getId(), owner1.getId());
        Optional<Product> notFoundProduct = productRepository.findByIdAndOwnerId(
                savedProduct.getId(), owner2.getId());
        
        assertTrue(foundProduct.isPresent());
        assertEquals(savedProduct.getId(), foundProduct.get().getId());
        assertEquals(owner1.getId(), foundProduct.get().getOwner().getId());
        
        assertFalse(notFoundProduct.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenProductNotFoundByIdAndOwnerId() {
        UUID nonExistentId = UUID.randomUUID();        
        Optional<Product> foundProduct = productRepository.findByIdAndOwnerId(
                nonExistentId, owner1.getId());
        

        assertFalse(foundProduct.isPresent());
    }

    @Test
    void shouldCheckIfProductExistsByOwnerIdAndNameIgnoreCase() {
        Product savedProduct = productRepository.save(testProduct);
        assertTrue(productRepository.existsByOwnerIdAndNameIgnoreCase(
                owner1.getId(), "Test Product"));
        assertTrue(productRepository.existsByOwnerIdAndNameIgnoreCase(
                owner1.getId(), "TEST PRODUCT"));
        assertTrue(productRepository.existsByOwnerIdAndNameIgnoreCase(
                owner1.getId(), "test product"));
        assertTrue(productRepository.existsByOwnerIdAndNameIgnoreCase(
                owner1.getId(), "Test Product"));
        
        assertFalse(productRepository.existsByOwnerIdAndNameIgnoreCase(
                owner2.getId(), "Test Product"));
        assertFalse(productRepository.existsByOwnerIdAndNameIgnoreCase(
                owner1.getId(), "Non Existent Product"));
    }

    @Test
    void shouldHandleUniqueConstraintForOwnerAndProductName() {
        productRepository.save(testProduct);
        
        Product duplicateNameProduct = Product.builder()
                .owner(owner1)
                .name("Test Product")
                .price(new BigDecimal("19.99"))
                .build();
        
        assertThrows(Exception.class, () -> {
            productRepository.save(duplicateNameProduct);
            productRepository.flush();
        });
    }

    @Test
    void shouldAllowSameProductNameForDifferentOwners() {
        Product product1 = Product.builder()
                .owner(owner1)
                .name("Same Name Product")
                .price(new BigDecimal("29.99"))
                .build();
        
        Product product2 = Product.builder()
                .owner(owner2)
                .name("Same Name Product")
                .price(new BigDecimal("39.99"))
                .build();
        
        Product savedProduct1 = productRepository.save(product1);
        Product savedProduct2 = productRepository.save(product2);
        assertNotNull(savedProduct1.getId());
        assertNotNull(savedProduct2.getId());
        assertEquals("Same Name Product", savedProduct1.getName());
        assertEquals("Same Name Product", savedProduct2.getName());
        assertNotEquals(savedProduct1.getOwner().getId(), savedProduct2.getOwner().getId());
    }

    @Test
    void shouldSaveProductWithNullDescription() {
        Product productWithoutDescription = Product.builder()
                .owner(owner1)
                .name("Product Without Description")
                .price(new BigDecimal("15.99"))
                .build();
        Product savedProduct = productRepository.save(productWithoutDescription);
        assertNotNull(savedProduct.getId());
        assertEquals("Product Without Description", savedProduct.getName());
        assertNull(savedProduct.getDescription());
        assertEquals(0, new BigDecimal("15.99").compareTo(savedProduct.getPrice()));
    }

    @Test
    void shouldHandleDifferentPriceFormats() {
        Product product1 = Product.builder()
                .owner(owner1)
                .name("Integer Price Product")
                .price(new BigDecimal("100"))
                .build();
        
        Product product2 = Product.builder()
                .owner(owner1)
                .name("Decimal Price Product")
                .price(new BigDecimal("99.99"))
                .build();
        
        Product product3 = Product.builder()
                .owner(owner1)
                .name("Zero Price Product")
                .price(BigDecimal.ZERO)
                .build();
        
        Product savedProduct1 = productRepository.save(product1);
        Product savedProduct2 = productRepository.save(product2);
        Product savedProduct3 = productRepository.save(product3);
        
        assertEquals(0, new BigDecimal("100.00").compareTo(savedProduct1.getPrice()));
        assertEquals(0, new BigDecimal("99.99").compareTo(savedProduct2.getPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(savedProduct3.getPrice()));
    }

    @Test
    void shouldUpdateExistingProduct() {
        Product savedProduct = productRepository.save(testProduct);
        UUID originalId = savedProduct.getId();
        savedProduct.setName("Updated Product");
        savedProduct.setDescription("Updated Description");
        savedProduct.setPrice(new BigDecimal("49.99"));
        Product updatedProduct = productRepository.save(savedProduct);
        assertEquals(originalId, updatedProduct.getId());
        assertEquals("Updated Product", updatedProduct.getName());
        assertEquals("Updated Description", updatedProduct.getDescription());
        assertEquals(0, new BigDecimal("49.99").compareTo(updatedProduct.getPrice()));
    }

    @Test
    void shouldDeleteProduct() {
        Product savedProduct = productRepository.save(testProduct);
        UUID productId = savedProduct.getId();
        productRepository.delete(savedProduct);
        Optional<Product> deletedProduct = productRepository.findById(productId);
        assertFalse(deletedProduct.isPresent());
    }

    @Test
    void shouldFindAllProducts() {
        Product product1 = Product.builder()
                .owner(owner1)
                .name("Product A")
                .price(new BigDecimal("10.00"))
                .build();
        
        Product product2 = Product.builder()
                .owner(owner2)
                .name("Product B")
                .price(new BigDecimal("20.00"))
                .build();
        
        productRepository.save(product1);
        productRepository.save(product2);
        
        List<Product> allProducts = productRepository.findAll();
        
        assertTrue(allProducts.size() >= 2);
        assertTrue(allProducts.stream().anyMatch(p -> "Product A".equals(p.getName())));
        assertTrue(allProducts.stream().anyMatch(p -> "Product B".equals(p.getName())));
    }

    @Test
    void shouldNotOverrideExistingIdInPrePersist() {
        // This test covers the branch where id != null in the @PrePersist method
        UUID predefinedId = UUID.randomUUID();
        Product productWithPredefinedId = Product.builder()
                .id(predefinedId)  // Setting ID explicitly
                .owner(owner1)
                .name("Product with Predefined ID")
                .price(new BigDecimal("25.99"))
                .build();
        
        Product savedProduct = productRepository.save(productWithPredefinedId);
        
        // The ID should remain the same as the predefined one
        assertEquals(predefinedId, savedProduct.getId());
        assertEquals("Product with Predefined ID", savedProduct.getName());
    }
}