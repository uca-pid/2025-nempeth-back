package com.nempeth.korven.service;

import com.nempeth.korven.constants.CategoryType;
import com.nempeth.korven.constants.MembershipRole;
import com.nempeth.korven.constants.MembershipStatus;
import com.nempeth.korven.persistence.entity.Business;
import com.nempeth.korven.persistence.entity.BusinessMembership;
import com.nempeth.korven.persistence.entity.Category;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.BusinessMembershipRepository;
import com.nempeth.korven.persistence.repository.BusinessRepository;
import com.nempeth.korven.persistence.repository.CategoryRepository;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.CategoryResponse;
import com.nempeth.korven.rest.dto.CreateCategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByBusiness(String userEmail, UUID businessId) {
        validateUserBusinessAccess(userEmail, businessId);
        
        return categoryRepository.findByBusinessId(businessId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCustomCategoriesByBusiness(String userEmail, UUID businessId) {
        validateUserBusinessAccess(userEmail, businessId);
        
        return categoryRepository.findByBusinessIdAndType(businessId, CategoryType.CUSTOM).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCustomCategory(String userEmail, UUID businessId, CreateCategoryRequest request) {
        validateUserBusinessAccess(userEmail, businessId);
        
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Negocio no encontrado"));
        
        if (categoryRepository.existsByBusinessIdAndNameIgnoreCase(businessId, request.name())) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
        
        Category category = Category.builder()
                .business(business)
                .name(request.name())
                .type(CategoryType.CUSTOM)
                .displayName(request.displayName())
                .icon(request.icon())
                .build();
        
        category = categoryRepository.save(category);
        
        return mapToResponse(category);
    }

    @Transactional
    public void deleteCustomCategory(String userEmail, UUID businessId, UUID categoryId) {
        validateUserBusinessAccess(userEmail, businessId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        
        if (!category.getBusiness().getId().equals(businessId)) {
            throw new IllegalArgumentException("La categoría no pertenece a este negocio");
        }
        
        if (category.getType() == CategoryType.STATIC) {
            throw new IllegalArgumentException("No se puede eliminar una categoría estática");
        }
        
        categoryRepository.delete(category);
    }

    private void validateUserBusinessAccess(String userEmail, UUID businessId) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        BusinessMembership membership = membershipRepository.findByBusinessIdAndUserId(businessId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tienes acceso a este negocio"));
        
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalArgumentException("Tu membresía en este negocio no está activa");
        }
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .displayName(category.getDisplayName())
                .icon(category.getIcon())
                .build();
    }
}