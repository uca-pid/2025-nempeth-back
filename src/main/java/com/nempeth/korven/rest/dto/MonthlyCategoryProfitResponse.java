package com.nempeth.korven.rest.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.YearMonth;

@Builder
public record MonthlyCategoryProfitResponse(
        YearMonth month,
        String categoryName,
        BigDecimal profit
) {
}