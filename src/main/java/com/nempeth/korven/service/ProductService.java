package com.nempeth.korven.service;

import com.nempeth.korven.constants.Role;
import com.nempeth.korven.persistence.entity.Product;
import com.nempeth.korven.persistence.entity.User;
import com.nempeth.korven.persistence.repository.ProductRepository;
import com.nempeth.korven.persistence.repository.UserRepository;
import com.nempeth.korven.rest.dto.ProductResponse;
import com.nempeth.korven.rest.dto.ProductUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID create(UUID ownerId, ProductUpsertRequest req) {
        User owner = mustFindOwner(ownerId);

        if (productRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, req.name())) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre para este dueño");
        }

        Product p = Product.builder()
                .owner(owner)
                .name(req.name())
                .description(req.description())
                .price(req.price())
                .build();
        productRepository.save(p);
        return p.getId();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listByOwner(UUID ownerId) {
        User owner = mustFindOwner(ownerId);
        return productRepository.findByOwner(owner).stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice()))
                .toList();
    }

    @Transactional
    public void update(UUID ownerId, UUID productId, ProductUpsertRequest req) {
        Product p = productRepository.findByIdAndOwnerId(productId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para este dueño"));
        p.setName(req.name());
        p.setDescription(req.description());
        p.setPrice(req.price());
        productRepository.save(p);
    }

    @Transactional
    public void delete(UUID ownerId, UUID productId) {
        Product p = productRepository.findByIdAndOwnerId(productId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para este dueño"));
        productRepository.delete(p);
    }

    private User mustFindOwner(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Dueño no encontrado"));
        if (owner.getRole() != Role.OWNER) {
            throw new IllegalArgumentException("El usuario no es dueño");
        }
        return owner;
    }
}
