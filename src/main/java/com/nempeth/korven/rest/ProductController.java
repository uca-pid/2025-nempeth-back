package com.nempeth.korven.rest;

import com.nempeth.korven.rest.dto.ProductResponse;
import com.nempeth.korven.rest.dto.ProductUpsertRequest;
import com.nempeth.korven.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Nota: ownerId se pasa por query param por simplicidad. En un futuro cambiar a JWT.
    @PostMapping
    public ResponseEntity<?> create(@RequestParam UUID ownerId, @RequestBody ProductUpsertRequest req) {
        UUID id = productService.create(ownerId, req);
        return ResponseEntity.ok(Map.of("productId", id.toString()));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestParam UUID ownerId) {
        return ResponseEntity.ok(productService.listByOwner(ownerId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> update(@RequestParam UUID ownerId,
                                    @PathVariable UUID productId,
                                    @RequestBody ProductUpsertRequest req) {
        productService.update(ownerId, productId, req);
        return ResponseEntity.ok(Map.of("message", "Producto actualizado"));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> delete(@RequestParam UUID ownerId, @PathVariable UUID productId) {
        productService.delete(ownerId, productId);
        return ResponseEntity.ok(Map.of("message", "Producto eliminado"));
    }
}
