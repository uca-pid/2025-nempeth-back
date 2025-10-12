package com.nempeth.korven.rest.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Builder
public record MonthlyCategoryRevenueResponse(
        YearMonth month,
        UUID categoryId,
        String categoryName,
        BigDecimal revenue
) {
}