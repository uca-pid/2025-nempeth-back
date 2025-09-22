package com.nempeth.korven.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nempeth.korven.config.TestMailConfiguration;
import com.nempeth.korven.constants.Role;
import com.nempeth.korven.rest.dto.LoginRequest;
import com.nempeth.korven.rest.dto.ProductUpsertRequest;
import com.nempeth.korven.rest.dto.RegisterRequest;
import com.nempeth.korven.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestMailConfiguration.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID ownerId;
    private String ownerToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        RegisterRequest ownerRequest = new RegisterRequest("owner@test.com", "Owner", "User", "password123", Role.OWNER);
        ownerId = authService.register(ownerRequest);
        LoginRequest ownerLogin = new LoginRequest("owner@test.com", "password123");
        ownerToken = authService.loginAndIssueToken(ownerLogin);

        RegisterRequest userRequest = new RegisterRequest("user@test.com", "Regular", "User", "password123", Role.USER);
        authService.register(userRequest);
        LoginRequest userLogin = new LoginRequest("user@test.com", "password123");
        userToken = authService.loginAndIssueToken(userLogin);
    }

    @Test
    void createProduct_asOwner_shouldCreateProduct() throws Exception {
        ProductUpsertRequest request = new ProductUpsertRequest("Test Product", "Test Description", new BigDecimal("29.99"));

        mockMvc.perform(post("/products")
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", not(emptyString())));
    }

    @Test
    void createProduct_asUser_shouldReturn403() throws Exception {
        ProductUpsertRequest request = new ProductUpsertRequest("Test Product", "Test Description", new BigDecimal("29.99"));

        mockMvc.perform(post("/products")
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_withoutAuth_shouldReturn403() throws Exception {
        ProductUpsertRequest request = new ProductUpsertRequest("Test Product", "Test Description", new BigDecimal("29.99"));

        mockMvc.perform(post("/products")
                .param("ownerId", ownerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listProducts_asOwner_shouldReturnProducts() throws Exception {
        ProductUpsertRequest request = new ProductUpsertRequest("List Product", "Description", new BigDecimal("19.99"));
        mockMvc.perform(post("/products")
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products")
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("List Product")))
                .andExpect(jsonPath("$[0].description", is("Description")))
                .andExpect(jsonPath("$[0].price", is(19.99)));
    }

    @Test
    void listProducts_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/products")
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listProducts_withoutAuth_shouldReturn403() throws Exception {
        mockMvc.perform(get("/products")
                .param("ownerId", ownerId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProduct_asOwner_shouldUpdateProduct() throws Exception {
        ProductUpsertRequest createRequest = new ProductUpsertRequest("Original Product", "Original Description", new BigDecimal("10.00"));
        String response = mockMvc.perform(post("/products")
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String productId = objectMapper.readTree(response).get("productId").asText();

        ProductUpsertRequest updateRequest = new ProductUpsertRequest("Updated Product", "Updated Description", new BigDecimal("15.00"));
        mockMvc.perform(put("/products/{productId}", productId)
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Producto actualizado")));
    }

    @Test
    void updateProduct_asUser_shouldReturn403() throws Exception {
        UUID randomProductId = UUID.randomUUID();
        ProductUpsertRequest updateRequest = new ProductUpsertRequest("Updated Product", "Updated Description", new BigDecimal("15.00"));
        
        mockMvc.perform(put("/products/{productId}", randomProductId)
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProduct_withoutAuth_shouldReturn403() throws Exception {
        UUID randomProductId = UUID.randomUUID();
        ProductUpsertRequest updateRequest = new ProductUpsertRequest("Updated Product", "Updated Description", new BigDecimal("15.00"));
        
        mockMvc.perform(put("/products/{productId}", randomProductId)
                .param("ownerId", ownerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_asOwner_shouldDeleteProduct() throws Exception {
        ProductUpsertRequest createRequest = new ProductUpsertRequest("Delete Product", "Delete Description", new BigDecimal("5.00"));
        String response = mockMvc.perform(post("/products")
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String productId = objectMapper.readTree(response).get("productId").asText();

        mockMvc.perform(delete("/products/{productId}", productId)
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Producto eliminado")));
    }

    @Test
    void deleteProduct_asUser_shouldReturn403() throws Exception {
        UUID randomProductId = UUID.randomUUID();
        
        mockMvc.perform(delete("/products/{productId}", randomProductId)
                .param("ownerId", ownerId.toString())
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_withoutAuth_shouldReturn403() throws Exception {
        UUID randomProductId = UUID.randomUUID();
        
        mockMvc.perform(delete("/products/{productId}", randomProductId)
                .param("ownerId", ownerId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_withMissingOwnerId_shouldReturn400() throws Exception {
        ProductUpsertRequest request = new ProductUpsertRequest("Test Product", "Test Description", new BigDecimal("29.99"));

        mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listProducts_withMissingOwnerId_shouldReturn400() throws Exception {
        mockMvc.perform(get("/products")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }
}