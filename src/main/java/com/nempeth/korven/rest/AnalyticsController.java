package com.nempeth.korven.rest;

import com.nempeth.korven.rest.dto.*;
import com.nempeth.korven.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/businesses/{businessId}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/revenue/by-category")
    public ResponseEntity<List<MonthlyCategoryRevenueResponse>> getMonthlyRevenueByCategory(
            @PathVariable UUID businessId,
            @RequestParam(required = false, defaultValue = "#{T(java.time.Year).now().getValue()}") Integer year,
            Authentication auth) {
        
        String userEmail = auth.getName();
        List<MonthlyCategoryRevenueResponse> revenue = analyticsService.getMonthlyRevenueByCategory(
                userEmail, businessId, year);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/profit/by-category")
    public ResponseEntity<List<MonthlyCategoryProfitResponse>> getMonthlyProfitByCategory(
            @PathVariable UUID businessId,
            @RequestParam(required = false, defaultValue = "#{T(java.time.Year).now().getValue()}") Integer year,
            Authentication auth) {
        
        String userEmail = auth.getName();
        List<MonthlyCategoryProfitResponse> profit = analyticsService.getMonthlyProfitByCategory(
                userEmail, businessId, year);
        return ResponseEntity.ok(profit);
    }

    @GetMapping("/revenue/total")
    public ResponseEntity<List<MonthlyRevenueResponse>> getMonthlyTotalRevenue(
            @PathVariable UUID businessId,
            @RequestParam(required = false, defaultValue = "#{T(java.time.Year).now().getValue()}") Integer year,
            Authentication auth) {
        
        String userEmail = auth.getName();
        List<MonthlyRevenueResponse> revenue = analyticsService.getMonthlyTotalRevenue(
                userEmail, businessId, year);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/profit/total")
    public ResponseEntity<List<MonthlyProfitResponse>> getMonthlyTotalProfit(
            @PathVariable UUID businessId,
            @RequestParam(required = false, defaultValue = "#{T(java.time.Year).now().getValue()}") Integer year,
            Authentication auth) {
        
        String userEmail = auth.getName();
        List<MonthlyProfitResponse> profit = analyticsService.getMonthlyTotalProfit(
                userEmail, businessId, year);
        return ResponseEntity.ok(profit);
    }
}