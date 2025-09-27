package com.nempeth.korven.rest;

import com.nempeth.korven.rest.dto.CategoryResponse;
import com.nempeth.korven.rest.dto.CreateCategoryRequest;
import com.nempeth.korven.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/businesses/{businessId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(@PathVariable UUID businessId,
                                                                  Authentication auth) {
        String userEmail = auth.getName();
        List<CategoryResponse> categories = categoryService.getCategoriesByBusiness(userEmail, businessId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/custom")
    public ResponseEntity<List<CategoryResponse>> getCustomCategories(@PathVariable UUID businessId,
                                                                     Authentication auth) {
        String userEmail = auth.getName();
        List<CategoryResponse> categories = categoryService.getCustomCategoriesByBusiness(userEmail, businessId);
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<?> createCustomCategory(@PathVariable UUID businessId,
                                                 @Valid @RequestBody CreateCategoryRequest request,
                                                 Authentication auth) {
        String userEmail = auth.getName();
        CategoryResponse category = categoryService.createCustomCategory(userEmail, businessId, request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Categoría creada exitosamente",
                "category", category
        ));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteCustomCategory(@PathVariable UUID businessId,
                                                 @PathVariable UUID categoryId,
                                                 Authentication auth) {
        String userEmail = auth.getName();
        categoryService.deleteCustomCategory(userEmail, businessId, categoryId);
        
        return ResponseEntity.ok(Map.of("message", "Categoría eliminada exitosamente"));
    }
}