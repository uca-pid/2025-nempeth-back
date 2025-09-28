package com.nempeth.korven.rest;

import com.nempeth.korven.rest.dto.BusinessResponse;
import com.nempeth.korven.rest.dto.BusinessDetailResponse;
import com.nempeth.korven.rest.dto.CreateBusinessRequest;
import com.nempeth.korven.rest.dto.JoinBusinessRequest;
import com.nempeth.korven.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping
    public ResponseEntity<?> createBusiness(@Valid @RequestBody CreateBusinessRequest request,
                                           Authentication auth) {
        String userEmail = auth.getName();
        BusinessResponse business = businessService.createBusiness(userEmail, request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Negocio creado exitosamente",
                "business", business
        ));
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinBusiness(@Valid @RequestBody JoinBusinessRequest request,
                                         Authentication auth) {
        String userEmail = auth.getName();
        BusinessResponse business = businessService.joinBusiness(userEmail, request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Te has unido al negocio exitosamente",
                "business", business
        ));
    }

    @GetMapping("/{businessId}/detail")
    public ResponseEntity<BusinessDetailResponse> getBusinessDetail(@PathVariable UUID businessId,
                                                                   Authentication auth) {
        String userEmail = auth.getName();
        BusinessDetailResponse businessDetail = businessService.getBusinessDetail(userEmail, businessId);
        
        return ResponseEntity.ok(businessDetail);
    }
}