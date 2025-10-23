package com.nempeth.korven.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        UUID businessId,
        String name,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal totalRevenueGoal,
        Boolean isLocked,
        Boolean isPeriodActive,
        Boolean isPeriodFinished,
        List<GoalCategoryTargetResponse> categoryTargets
) {
}
