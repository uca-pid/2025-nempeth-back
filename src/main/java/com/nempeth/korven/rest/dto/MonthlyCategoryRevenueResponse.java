package com.nempeth.korven.rest.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.YearMonth;

@Builder
public record MonthlyCategoryRevenueResponse(
        YearMonth month,
        String categoryName,
        BigDecimal revenue
) {
}