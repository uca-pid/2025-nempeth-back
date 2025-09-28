package com.nempeth.korven.service;

import com.nempeth.korven.constants.MembershipRole;
import com.nempeth.korven.constants.MembershipStatus;
import com.nempeth.korven.persistence.entity.Business;
import com.nempeth.korven.persistence.entity.BusinessMembership;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.BusinessRepository;
import com.nempeth.korven.persistence.repository.BusinessMembershipRepository;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.persistence.repository.CategoryRepository;
import com.nempeth.korven.persistence.repository.ProductRepository;
import com.nempeth.korven.persistence.repository.SaleRepository;
import com.nempeth.korven.rest.dto.BusinessResponse;
import com.nempeth.korven.rest.dto.BusinessDetailResponse;
import com.nempeth.korven.rest.dto.BusinessMemberDetailResponse;
import com.nempeth.korven.rest.dto.BusinessStatsResponse;
import com.nempeth.korven.rest.dto.CategoryResponse;
import com.nempeth.korven.rest.dto.ProductResponse;
import com.nempeth.korven.rest.dto.CreateBusinessRequest;
import com.nempeth.korven.rest.dto.JoinBusinessRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Random;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int JOIN_CODE_LENGTH = 8;

    @Transactional
    public BusinessResponse createBusiness(String userEmail, CreateBusinessRequest request) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // Generar código único
        String joinCode = generateUniqueJoinCode();
        
        Business business = Business.builder()
                .name(request.name())
                .joinCode(joinCode)
                .joinCodeEnabled(true)
                .build();
        
        business = businessRepository.save(business);
        
        // Crear membership como OWNER
        BusinessMembership membership = BusinessMembership.builder()
                .business(business)
                .user(user)
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build();
        
        membershipRepository.save(membership);
        
        return BusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .joinCode(business.getJoinCode())
                .joinCodeEnabled(business.getJoinCodeEnabled())
                .build();
    }

    @Transactional
    public BusinessResponse joinBusiness(String userEmail, JoinBusinessRequest request) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        Business business = businessRepository.findByJoinCode(request.joinCode())
                .orElseThrow(() -> new IllegalArgumentException("Código de acceso inválido"));
        
        if (!business.getJoinCodeEnabled()) {
            throw new IllegalArgumentException("El código de acceso está deshabilitado");
        }
        
        // Verificar si ya es miembro
        if (membershipRepository.existsByBusinessIdAndUserId(business.getId(), user.getId())) {
            throw new IllegalArgumentException("Ya eres miembro de este negocio");
        }
        
        // Crear membership como EMPLOYEE
        BusinessMembership membership = BusinessMembership.builder()
                .business(business)
                .user(user)
                .role(MembershipRole.EMPLOYEE)
                .status(MembershipStatus.ACTIVE)
                .build();
        
        membershipRepository.save(membership);
        
        return BusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .joinCode(business.getJoinCode())
                .joinCodeEnabled(business.getJoinCodeEnabled())
                .build();
    }

    @Transactional(readOnly = true)
    public BusinessDetailResponse getBusinessDetail(String userEmail, UUID businessId) {
        // Validar acceso del usuario al negocio
        validateUserBusinessAccess(userEmail, businessId);
        
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Negocio no encontrado"));
        
        // Obtener miembros del negocio
        List<BusinessMemberDetailResponse> members = membershipRepository.findByBusinessIdAndStatus(businessId, MembershipStatus.ACTIVE)
                .stream()
                .map(this::mapToMemberDetailResponse)
                .toList();
        
        // Obtener categorías
        List<CategoryResponse> categories = categoryRepository.findByBusinessId(businessId)
                .stream()
                .map(this::mapToCategoryResponse)
                .toList();
        
        // Obtener productos
        List<ProductResponse> products = productRepository.findByBusinessId(businessId)
                .stream()
                .map(this::mapToProductResponse)
                .toList();
        
        // Calcular estadísticas
        BusinessStatsResponse stats = calculateBusinessStats(businessId);
        
        return BusinessDetailResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .joinCode(business.getJoinCode())
                .joinCodeEnabled(business.getJoinCodeEnabled())
                .members(members)
                .categories(categories)
                .products(products)
                .stats(stats)
                .build();
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

    private BusinessMemberDetailResponse mapToMemberDetailResponse(BusinessMembership membership) {
        User user = membership.getUser();
        return BusinessMemberDetailResponse.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userName(user.getName())
                .userLastName(user.getLastName())
                .role(membership.getRole())
                .status(membership.getStatus())
                .build();
    }

    private CategoryResponse mapToCategoryResponse(com.nempeth.korven.persistence.entity.Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .displayName(category.getDisplayName())
                .icon(category.getIcon())
                .build();
    }

    private ProductResponse mapToProductResponse(com.nempeth.korven.persistence.entity.Product product) {
        CategoryResponse categoryResponse = mapToCategoryResponse(product.getCategory());
        
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(categoryResponse)
                .build();
    }

    private BusinessStatsResponse calculateBusinessStats(UUID businessId) {
        Long totalMembers = membershipRepository.findByBusinessIdAndStatus(businessId, MembershipStatus.ACTIVE)
                .stream()
                .count();
        
        Long totalCategories = categoryRepository.findByBusinessId(businessId)
                .stream()
                .count();
        
        Long totalProducts = productRepository.findByBusinessId(businessId)
                .stream()
                .count();
        
        // Obtener estadísticas de ventas
        List<com.nempeth.korven.persistence.entity.Sale> sales = saleRepository.findByBusinessIdOrderByOccurredAtDesc(businessId);
        
        java.math.BigDecimal totalRevenue = sales
                .stream()
                .map(com.nempeth.korven.persistence.entity.Sale::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        Long totalSales = (long) sales.size();
        
        return BusinessStatsResponse.builder()
                .totalMembers(totalMembers)
                .totalCategories(totalCategories)
                .totalProducts(totalProducts)
                .totalSales(totalSales)
                .totalRevenue(totalRevenue)
                .activeMembers(totalMembers) // Por ahora es lo mismo ya que solo obtenemos miembros activos
                .build();
    }

    private String generateUniqueJoinCode() {
        String joinCode;
        do {
            joinCode = generateRandomJoinCode();
        } while (businessRepository.existsByJoinCode(joinCode));
        
        return joinCode;
    }

    private String generateRandomJoinCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        
        return sb.toString();
    }
}