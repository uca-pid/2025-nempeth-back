package com.nempeth.korven.rest.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BusinessStatsResponse(
        Long totalMembers,
        Long totalCategories,
        Long totalProducts,
        Long totalSales,
        BigDecimal totalRevenue,
        Long activeMembers
) {
}