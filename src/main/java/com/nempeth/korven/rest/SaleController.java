package com.nempeth.korven.rest;

import com.nempeth.korven.rest.dto.CreateSaleRequest;
import com.nempeth.korven.rest.dto.SaleResponse;
import com.nempeth.korven.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/businesses/{businessId}/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    public ResponseEntity<?> createSale(@PathVariable UUID businessId,
                                       @Valid @RequestBody CreateSaleRequest request,
                                       Authentication auth) {
        String userEmail = auth.getName();
        UUID saleId = saleService.createSale(userEmail, businessId, request);
        
        return ResponseEntity.ok(Map.of(
                "message", "Venta registrada exitosamente",
                "saleId", saleId.toString()
        ));
    }

    @GetMapping
    public ResponseEntity<List<SaleResponse>> getSales(@PathVariable UUID businessId,
                                                      @RequestParam(required = false) 
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                      OffsetDateTime startDate,
                                                      @RequestParam(required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                      OffsetDateTime endDate,
                                                      Authentication auth) {
        String userEmail = auth.getName();
        List<SaleResponse> sales;
        
        if (startDate != null && endDate != null) {
            sales = saleService.getSalesByBusinessAndDateRange(userEmail, businessId, startDate, endDate);
        } else {
            sales = saleService.getSalesByBusiness(userEmail, businessId);
        }
        
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/{saleId}")
    public ResponseEntity<SaleResponse> getSaleById(@PathVariable UUID businessId,
                                                   @PathVariable UUID saleId,
                                                   Authentication auth) {
        String userEmail = auth.getName();
        SaleResponse sale = saleService.getSaleById(userEmail, businessId, saleId);
        return ResponseEntity.ok(sale);
    }
}